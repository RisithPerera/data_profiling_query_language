package de.metathesis;

import de.metanome.MetanomeHelper;
import de.metanome.algorithm_integration.AlgorithmConfigurationException;
import de.metanome.algorithm_integration.input.InputGenerationException;
import de.metanome.algorithm_integration.input.RelationalInput;
import de.metanome.algorithm_integration.input.RelationalInputGenerator;
import de.metaserve.util.singletons.InputConfigurationSingleton;
import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.PositionListIndex;
import de.metathesis.structures.Relation;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.math.BigInteger;
import java.util.*;


public final class Preprocessor {

    private static final Preprocessor INSTANCE = new Preprocessor();
    private static final double CACHE_MEMORY_THRESHOLD = 0.8;

    private final Int2ObjectMap<Relation> relationMap = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<Sampler2> sampler2Map = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<ValidatorNew> validatorMap = new Int2ObjectOpenHashMap<>();

    //This structure cache n-ary PLIs temporary and remove least recently used one.
    private final LinkedHashMap<AttributeBitSet, PositionListIndex> pliCache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<AttributeBitSet, PositionListIndex> eldest) {
            return MemoryUtils.systemMemoryUsage() > CACHE_MEMORY_THRESHOLD;
        }
    };

    private final int inputRowLimit;
    private final boolean isNullEqualNull;
    private final String nullValue;

    private Preprocessor() {
        this.inputRowLimit = InputConfigurationSingleton.get().getFILE_MAX_ROWS();
        this.isNullEqualNull = InputConfigurationSingleton.get().getFILE_NULL_EQUALS_NULL();
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

    public AttributeBitSet[] generateApriori(int relationIndex, int level) {
        int cols = this.relationMap.get(relationIndex).getNumOfAttributes();

        if (level < 0 ||  level > cols) {
            throw new IllegalArgumentException("Level " + level + " must be between column boundary (0:" + cols + ") of relation:" + relationIndex);
        }

        if(level == 0) {
            return new AttributeBitSet[]{new AttributeBitSet(relationIndex, new BitSet())};
        }

        int count = Utility.binomial(cols, level);
        AttributeBitSet[] result = new AttributeBitSet[count];

        BigInteger mask = BigInteger.ONE.shiftLeft(level).subtract(BigInteger.ONE);  // first combination
        BigInteger limit = BigInteger.ONE.shiftLeft(cols);

        int idx = 0;
        while (mask.compareTo(limit) < 0) {
            BitSet bitSet = Utility.toBitSet(mask, cols);

            result[idx++] = new AttributeBitSet(relationIndex, bitSet);

            // Gosper's hack for BigInteger
            BigInteger c = mask.and(mask.negate());
            BigInteger r = mask.add(c);
            mask = r.or(r.xor(mask).shiftRight(2).divide(c));
        }

        return result;
    }

    public List<BitSet> produceSubSets(BitSet superSet, int level) {
        int[] bits = superSet.stream().toArray();
        int n = bits.length;

        if (level > n) throw new IllegalArgumentException(
                "Level " + level + " exceeds superSet size " + n
        );

        if (level == n) return List.of((BitSet) superSet.clone());

        List<BitSet> result = new ArrayList<>();

        int mask = (1 << level) - 1;
        int limit = (1 << n);

        while (mask < limit) {
            BitSet subset = new BitSet();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    subset.set(bits[i]); // map back to actual bit positions
                }
            }
            result.add(subset);

            // Gosper's hack
            int c = mask & (-mask);
            int r = mask + c;
            mask = (((r ^ mask) >> 2) / c) | r;
        }

        return result;
    }

    public Relation getRelation(int relationIndex) {
        assert this.relationMap.containsKey(relationIndex) : "Relation not available!";

        //TODO: For INDs loading only data is enough. PLI is not needed. Need to tackle it.
        loadRelationData(relationIndex);

        return this.relationMap.get(relationIndex);
    }

    public Sampler2 getSampler2(int relationIndex){
        return this.sampler2Map.computeIfAbsent(relationIndex, k -> new Sampler2(getRelation(k)));
    }

    public ValidatorNew getValidator(int relationIndex){
        return this.validatorMap.computeIfAbsent(relationIndex, k -> new ValidatorNew(getRelation(k)));
    }

    public synchronized PositionListIndex getPLI(AttributeBitSet abs) {

        loadRelationData(abs.getRelationIndex());

        if (abs.size() == 1) {
            int attrIndex = abs.getAttributeIndexSet().nextSetBit(0);
            return this.relationMap.get(abs.getRelationIndex()).getUnaryPLIs()[attrIndex];
        }

        // Check LRU cache first
        PositionListIndex cached = pliCache.get(abs);
        if (cached != null) return cached;

        Relation relation = this.relationMap.get(abs.getRelationIndex());

        // Compute by intersecting unary PLIs
        PositionListIndex result = null;
        PositionListIndex[] unary = relation.getUnaryPLIs();

        for (int index : abs.getAttributeIndexSet().stream().toArray()) {
            if (result == null) {
                result = unary[index];
            } else {
                result = result.intersect(unary[index]);
            }
        }

        // Cache only if memory allows
        if (MemoryUtils.systemMemoryUsage() < CACHE_MEMORY_THRESHOLD) {
            pliCache.put(abs, result);
        }

        return result;
    }

    private void loadRelationData(int relationIndex) {
        Relation relation = this.relationMap.get(relationIndex);

        if (relation.isDataLoaded()){
            return; // already loaded
        }

        int numAttributes = relation.getNumOfAttributes();

        try(RelationalInput relationalInput = this.relationMap.get(relationIndex).getRelationalInput()){
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
                    String value =  record.get(c);
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

            // Build unary PLIs
            PositionListIndex[] unaryPLIs = buildUnaryPLIs(relationIndex, relationData);

            // Build compressed records for sampling
            int[][] compressed = buildCompressedRecords(unaryPLIs, numOfRecords);

            //Sorting PLIs Based on Cluster Size
            rearrangeUnaryPLIs(unaryPLIs, numOfRecords, compressed);

            relation.markLoaded(relationData, unaryPLIs,  compressed);
        }catch (Exception e) {
            throw new RuntimeException("Issue with Relation Loading Process", e);
        }
    }

    private PositionListIndex[] buildUnaryPLIs(int relationIndex, String[][] columns){
        int numOfAttributes = columns.length;

        PositionListIndex[] plis = new PositionListIndex[numOfAttributes];
        for (int columnIndex = 0; columnIndex < numOfAttributes; columnIndex++) {
            Map<String, IntArrayList> clusterMap = new HashMap<>();

            int rowIndex = 0;
            for(String value : columns[columnIndex]) {
                if (Objects.equals(value, this.nullValue) && !this.isNullEqualNull) {
                    rowIndex++;
                    continue;
                }
                clusterMap.computeIfAbsent(value, k -> new IntArrayList()).add(rowIndex);
                rowIndex++;
            }

            List<IntArrayList> clusters = new ArrayList<>();
            int numUniqueValues = 0;
            for (IntArrayList cluster : clusterMap.values()) {
                if (cluster.size() > 1) {
                    clusters.add(cluster);
                }else{
                    numUniqueValues++;
                }
            }

            AttributeBitSet attributeBitSet = new AttributeBitSet(relationIndex, columnIndex);

            PositionListIndex pli = new PositionListIndex(attributeBitSet, clusters);
            pli.setNumUniqueValues(numUniqueValues);
            pli.setNumOfRecords(columns[0].length);

            plis[columnIndex] = pli;
        }

        return plis;
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

    private static int[][] buildCompressedRecords(PositionListIndex[] plis, int numOfRecords) {
        // Direct [row][col] matrix
        int[][] compressedRecords = new int[numOfRecords][plis.length];

        // Fill all with -1 (unique/singleton values)
        for (int[] row : compressedRecords) {
            Arrays.fill(row, -1);
        }

        // For each column's PLI, assign cluster IDs directly into row-col position
        for (int attr = 0; attr < plis.length; attr++) {
            List<IntArrayList> clusters = plis[attr].getClusters();
            for (int clusterId = 0; clusterId < clusters.size(); clusterId++) {
                for (int recordId : clusters.get(clusterId)) {
                    compressedRecords[recordId][attr] = clusterId;
                }
            }
        }

        return compressedRecords;
    }
}