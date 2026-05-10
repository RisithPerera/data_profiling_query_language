package de.metathesis;

import de.metanome.MetanomeHelper;
import de.metanome.algorithm_integration.AlgorithmConfigurationException;
import de.metanome.algorithm_integration.input.InputGenerationException;
import de.metanome.algorithm_integration.input.RelationalInput;
import de.metanome.algorithm_integration.input.RelationalInputGenerator;
import de.metaserve.util.singletons.InputConfigurationSingleton;
import de.metathesis.profilers.sampling.Sampler;
import de.metathesis.profilers.structures.INDUnaryCover;
import de.metathesis.profilers.validators.FDValidator;
import de.metathesis.profilers.validators.UCCValidator;
import de.metathesis.structures.PositionListIndex;
import de.metathesis.structures.Relation;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Preprocessor {
    private static final Logger log = LogManager.getLogger(Preprocessor.class);

    private static final Preprocessor INSTANCE = new Preprocessor();

    private final Map<Integer, Relation> relationMap = new ConcurrentHashMap<>();
    private final Map<Integer, Sampler> samplerMap = new ConcurrentHashMap<>();
    private final Map<Integer, FDValidator> fdValidatorMap = new ConcurrentHashMap<>();
    private final Map<Integer, UCCValidator> uccValidatorMap = new ConcurrentHashMap<>();
    @Getter
    private final INDUnaryCover indUnaryCover = new INDUnaryCover();

    private final int inputRowLimit;
    private final String nullValue;

    private Preprocessor() {
        this.inputRowLimit = InputConfigurationSingleton.get().getFILE_MAX_ROWS();
        this.nullValue = InputConfigurationSingleton.get().getFILE_NULL_STRING();
    }

    public static Preprocessor getInstance() {
        return INSTANCE;
    }

    public Map<String, int[]> initializeSearchSpace(Map<String, List<String>> relationNameMap) {
        Map<String, int[]> relationIndexMap = new HashMap<>(relationNameMap.size());

        for (Map.Entry<String, List<String>> entry : relationNameMap.entrySet()) {
            List<Integer> relationIndexList = new ArrayList<>();
            for (String relationName : entry.getValue()) {
                Relation relation = this.relationMap.values().stream()
                        .filter(r -> r.getName().equals(relationName))
                        .findFirst()
                        .orElseGet(() -> {
                            int id = this.relationMap.size();
                            try {
                                RelationalInputGenerator inputGenerator = MetanomeHelper.getInput(relationName);
                                RelationalInput relationalInput = inputGenerator.generateNewCopy();
                                assert Objects.nonNull(relationalInput) : "Input generation failed!";

                                Relation newRelation = new Relation(id, relationalInput);

                                this.relationMap.put(id, newRelation);
                                return newRelation;
                            } catch (InputGenerationException | AlgorithmConfigurationException e) {
                                throw new RuntimeException(e);
                            }
                        });
                relationIndexList.add(relation.getIndex());
            }

            relationIndexMap.put(entry.getKey(), relationIndexList.stream().mapToInt(Integer::intValue).toArray());
        }

        return relationIndexMap;
    }

    public synchronized Map<String, int[]> getAttributeSizesOf(Map<String, int[]> relationIndexesMap) {
        Map<String, int[]> relationSizesMap = new HashMap<>(relationIndexesMap.size());
        for (Map.Entry<String, int[]> relationsEntry : relationIndexesMap.entrySet()) {
            int[] sizes = new int[relationsEntry.getValue().length];

            for (int i = 0; i < relationsEntry.getValue().length; i++) {
                sizes[i] = this.relationMap.get(relationsEntry.getValue()[i]).getNumOfAttributes();
            }

            relationSizesMap.put(relationsEntry.getKey(), sizes);
        }
        return relationSizesMap;
    }

    public Relation getRelation(int relationIndex) {
        assert this.relationMap.containsKey(relationIndex) : "Relation not available!";
        Relation relation = this.relationMap.get(relationIndex);

        if (!relation.isDataLoaded()) {
            synchronized (relation){
                if (!relation.isDataLoaded()) {
                    long time = System.currentTimeMillis();
                    loadRelationData(relation);
                    log.info("Loaded Relation {}: {} in {}ms", relation.getIndex(), relation.getName(), System.currentTimeMillis() - time);
                }
            }
        }

        return relation;
    }

    public Sampler getSampler(int relationIndex){
        return this.samplerMap.computeIfAbsent(relationIndex, k -> new Sampler(getRelation(k)));
    }

    public FDValidator getFDValidator(int relationIndex){
        return this.fdValidatorMap.computeIfAbsent(relationIndex, k -> new FDValidator(getRelation(k)));
    }

    public UCCValidator getUCCValidator(int relationIndex){
        return this.uccValidatorMap.computeIfAbsent(relationIndex, k -> new UCCValidator(getRelation(k)));
    }

    private void loadRelationData(Relation relation) {
        int numAttributes = relation.getNumOfAttributes();

        try (RelationalInput relationalInput = relation.getRelationalInput()) {
            assert relationalInput != null : "Preprocessor Initialization Failed";

            int numOfRecords = 0;

            // temporary column-wise buffers
            ObjectArrayList<String>[] columns = new ObjectArrayList[numAttributes];
            for (int c = 0; c < numAttributes; c++) {
                columns[c] = new ObjectArrayList<>();
            }

            while (relationalInput.hasNext() && (this.inputRowLimit <= 0 || this.inputRowLimit > numOfRecords)) {
                List<String> record = relationalInput.next();

                for (int c = 0; c < numAttributes; c++) {
                    String value = record.get(c);
                    //columns[c].add((value == null || value.isEmpty() ? this.nullValue : value).intern());
                    columns[c].add(value == null || value.isEmpty() ? this.nullValue : value);
                }

                numOfRecords++;
                if (numOfRecords > Integer.MAX_VALUE - 1) {
                    throw new IllegalStateException("Number of records " + numOfRecords + "exceeds max int for IntArrayList. Use long-based PLIs instead");
                }
            }

            // materialize final column arrays
            String[][] relationData = new String[numAttributes][];
            for (int c = 0; c < numAttributes; c++) {
                relationData[c] = columns[c].toArray(new String[0]);
            }

            relation.loadData(relationData, numOfRecords);
        }catch (Exception e) {
            throw new RuntimeException("Issue with Relation Loading Process", e);
        }
    }

    private void rearrangeUnaryPLIs(PositionListIndex[] plis, int numOfRecords, int[][] compressed){
        // Sort plis by number of clusters: For searching in the covers and for validation,
        // it is good to have attributes with few non-unique values and many clusters left in the prefix tree

        // Step 1: Sort clusters within each PLI by size
        for (PositionListIndex pli : plis) {
            pli.getClusters().sort((c1, c2) -> {
                int cmp = c2.size() - c1.size();
                if (cmp != 0) return cmp;
                // tie-break: first element (clusters are internally sorted)
                return Integer.compare(c1.getInt(0), c2.getInt(0));
            });
        }

        // Sort 2: Sort PLIs by largest cluster //TODO: Need to think how to Keep PLI match with the original index
//        Arrays.sort(plis, (p1, p2) -> {
//            int max1 = p1.getClusters().isEmpty() ? 0 : p1.getClusters().getFirst().size();
//            int max2 = p2.getClusters().isEmpty() ? 0 : p2.getClusters().getFirst().size();
//            return max2 - max1;
//        });

//        // TODO: For Cluster Inside Sorting, needs to Check Whether Use the same approach as HyFD. Below using the Original version
//        // Sort 3: clusters by rowClusterCount descending
//        int[] rowClusterCount = new int[numOfRecords];
//        for (PositionListIndex pli : plis) {
//            for (IntArrayList cluster : pli.getClusters()) {
//                for (int rowId : cluster) rowClusterCount[rowId]++;
//            }
//        }
//
//        // Sort 4: Should — sort ROWS within each cluster
//        for (PositionListIndex pli : plis) {
//            for (IntArrayList cluster : pli.getClusters()) {
//                cluster.sort((r1, r2) -> rowClusterCount[r2] - rowClusterCount[r1]);
//            }
//        }

        //TODO: This will affect on PLI intersection because its assume cluster numbers are in order.
//        ClusterComparator comparator = new ClusterComparator(compressed, compressed[0].length - 1, 1);
//        for (PositionListIndex pli : plis) {
//            for (IntArrayList cluster : pli.getClusters()) {
//                cluster.sort(comparator);
//            }
//            comparator.incrementActiveKey();
//        }
    }

    public void clear(){
        relationMap.clear();
        samplerMap.clear();
        fdValidatorMap.clear();
        uccValidatorMap.clear();
        indUnaryCover.clear();
        System.gc();
    }
}