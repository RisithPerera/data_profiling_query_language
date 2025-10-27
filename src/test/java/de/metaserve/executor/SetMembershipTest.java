package de.metaserve.executor;

import de.metaserve.executor.min.*;
import de.metaserve.parser.graph.Condition;
import de.metaserve.parser.graph.FD;
import de.metaserve.parser.graph.IND;
import de.metaserve.parser.graph.UCC;
import de.metaserve.util.common.GraphGenerator;
import de.metaserve.util.common.GraphUtil;
import de.metaserve.util.common.Pair;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class SetMembershipTest {
    private static final String RESOURCES_PATH = "/resources/dpqlTest.csv";


    @Test
    void hashCodeTest(){
        assertNotEquals(new INDEdge("X", "Y").hashCode(), new INDEdge("Y", "X").hashCode());
        assertNotEquals(new FDEdge("X", "Y").hashCode(), new FDEdge("Y", "X").hashCode());
    }

    @Test
    void specificGraph(){
        Graph graph = GraphUtil.fromString("[UCC(A), IND(C,B), FD(B,C), FD(B,A), IND(B,C)]");
        graph.computeSetMembership();
    }


    @Test
    void testKSetMembership(){
        int k = 6;
        HashMap<GraphAtom, List<GraphAtom>> graphMap = Graph.decomposedGraphMap;
        for (int i = 1; i < k; i++){
            int numberOfUCCs = 0;
            int numberOfFDs = 0;
            int numberOfINDs = 0;
            Set<Graph> graphs = (new GraphGenerator()).generateGraphs(i, false);
            System.out.println(i + ": " + graphs.size());
            GraphMetadata metadata = new GraphMetadata();
            ResultMetadata resultMetadata2 = new ResultMetadata();
            ResultMetadata resultMetadata = new ResultMetadata();
            for (Graph graph : graphs){
                int fds = metadata.getFD_Decomposition();
                int inds = metadata.getIND_Decomposition();
                int numInds = this.inds;
                int numFds = this.fds;
                for (Edge edge : graph.getEdges()){
                    if (edge instanceof FDEdge)
                        numberOfFDs++;
                    if (edge instanceof INDEdge)
                        numberOfINDs++;
                    if (edge instanceof UCCEdge)
                        numberOfUCCs++;
                }
                checkNumberOfTypes(graph);
                graph.computeSetMembership();
                Set<String> results = setMemberShipToString(graph.getSetMembershipMap(), graph.getExistsNodes());
                //System.out.println(results);
                resultMetadata.add(results);
                resultMetadata2.add(graph.getSetMembershipMap());
                metadata.add(graph.getMetadata());
                fds = metadata.getFD_Decomposition() - fds;
                inds = metadata.getIND_Decomposition() - inds;
                numInds = this.inds - numInds;
                numFds = this.fds - numFds;
                if (inds < fds && numFds <= numInds)
                    System.out.println();
                if (numFds < fds)
                    System.out.println();
            }
            System.out.println(metadata);
            System.out.println(resultMetadata);
            System.out.println(resultMetadata2);
            System.out.println(i + " UCCs: " + numberOfUCCs);
            System.out.println(i + " FDs: " + numberOfFDs);
            System.out.println(i + " INDs: " + numberOfINDs);
        }
        printMap(graphMap);
        System.out.println("UCCs: " + uccs);
        System.out.println("FDs: " + fds);
        System.out.println("INDs: " + inds);

    }

    int uccs = 0;
    int fds = 0;
    int inds = 0;

    private void checkNumberOfTypes(Graph graph) {
        for (Edge edge : graph.getEdges()){
            if (edge instanceof FDEdge)
                fds++;
            if (edge instanceof INDEdge)
                inds++;
            if (edge instanceof UCCEdge)
                uccs++;
        }
    }

    int uccLHS = 0;
    int uccRHS = 0;
    private void checkGraph(Graph graph) {
        int lhsHash = 1447431019;
        int rhsHash = 457936155;
        int lhsSize = Graph.decomposedGraphMap.getOrDefault(new GraphAtom(lhsHash), new ArrayList<>()).size();
        int rhsSize = Graph.decomposedGraphMap.getOrDefault(new GraphAtom(rhsHash), new ArrayList<>()).size();
        int localLHS = 0;
        int localRHS = 0;
        for (Node node: graph.getNodes().values()){
            boolean ucc = false;
            int fdLHS = 0;
            int fdRHS = 0;
            for (Edge edge : node.edges()){
                if (edge instanceof UCCEdge) ucc = true;
                if (edge instanceof FDEdge && edge.leftName.equals(node.getName())) fdLHS++;
                if (edge instanceof FDEdge && edge.rightName.equals(node.getName())) fdRHS++;
            }
            if (ucc && fdLHS > 0) localLHS += fdLHS;
            if (ucc && fdRHS > 0) localRHS += fdRHS;
        }
        if(localRHS == localLHS){
            graph.computeSetMembership();
            int lhsDiff = Graph.decomposedGraphMap.getOrDefault(new GraphAtom(lhsHash), new ArrayList<>()).size() - lhsSize;
            int rhsDiff = Graph.decomposedGraphMap.getOrDefault(new GraphAtom(rhsHash), new ArrayList<>()).size() - rhsSize;
            if (lhsDiff != rhsDiff){
                graph.computeSetMembership();
            }
        }
        uccLHS += localLHS;
        uccRHS += localRHS;
    }

    public static void printMap(HashMap<GraphAtom, List<GraphAtom>> map) {
        for (GraphAtom key : map.keySet()) {
            List<GraphAtom> valueList = map.get(key);
            int size = (valueList != null) ? valueList.size() : 0;
            System.out.println("GraphID: " + key + ", Size: " + size + " Graph:" + key.graph + " ---  " + key.graph.getSetMembershipMap());
        }
    }

    @Test
    void testSetMembershipFromFile(){
        int i = 0;
        int skipUntil = 0;
        for (Pair<List<Condition>, Set<String>> pair : getTestFile()){
            i++;
            if (i < skipUntil) continue;
            List<Condition> conditions = pair.first();
            System.out.println(i + ": " + conditions);
            Set<String> expectedResults = pair.second();
            Graph graph = Graph.fromConditions(conditions);
            graph.computeSetMembership();
            Set<String> actualResults = setMemberShipToString(graph.getSetMembershipMap(), graph.getExistsNodes());
            actualResults = actualResults.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            expectedResults = expectedResults.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            assertEquals(expectedResults, actualResults);
        }
    }

    private Set<String> setMemberShipToString(HashMap<Edge, Graph.SetMembership> setMembershipMap, Set<String> existsNodes) {
        Set<String> result = new HashSet<>();
        for (Edge edge : setMembershipMap.keySet()) {
            Graph.SetMembership membership = setMembershipMap.get(edge);
            String stringMembership = Graph.SetMembership.toString(membership, edge);
            if (stringMembership.contains("and"))
                result.addAll(List.of(stringMembership.split("\\\\land")));
            else
                result.add(stringMembership);
        }
        if (!existsNodes.isEmpty()){
            for (Edge edge : setMembershipMap.keySet()){
                String nodeStringExists = Graph.SetMembership.toString(edge, existsNodes);
                result.add(nodeStringExists);
            }
            for (String nodeName : existsNodes){
                String subsetNotAllowed = nodeName + "'" + " \\subset " + nodeName;
                result.add(subsetNotAllowed);
            }
        }
        return result;
    }

    private List<Pair<List<Condition>, Set<String>>> getTestFile() {
        List<Pair<List<Condition>, Set<String>>> result = new ArrayList<>();
        for (String line : readFile()){
            String[] split = line.split("\",\"");
            List<Condition> conditions = getConditionFromString(split[0]);
            Set<String> set = getExpectedString(split[1]);
            result.add(new Pair<>(conditions, set));
        }
        return result;
    }

    private Set<String> getExpectedString(String s) {
        s = s.replace("\"", "");
        Set<String> result = new HashSet<>();
        String[] split = s.split(":", 2);
        String[] conditions = split[1].split("\\\\land");
        for (String condition : conditions){
            condition = condition.trim();
            if(condition.isBlank())
                continue;
            if(condition.contains("nexists")){
                String[] split1 = condition.replace("\\nexists [", "").split("] \\\\in q: ");
                result.addAll(List.of(split1[0].split(",")));
                result.addAll(List.of(split1[1].split("\\\\vee")));
            } else {
                result.add(condition);
            }
        }
        result = result.stream()
                .map(String::trim)
                .collect(Collectors.toSet());
        return result;
    }

    private List<String> readFile() {
        String currentDir = System.getProperty("user.dir");
        Path resourcesPath = Paths.get(currentDir, "src", "test", RESOURCES_PATH);
        List<String> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(resourcesPath.toFile()))) {
            String line;
            boolean skipFirstLine = true;
            while ((line = br.readLine()) != null) {
                if (skipFirstLine){
                    skipFirstLine = false;
                    continue;
                }
                result.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public List<Condition> getConditionFromString(String input){
        List<Condition> conditions = new ArrayList<>();
        String[] ands = input.split("AND");
        for (String stringCondition : ands){
            stringCondition = stringCondition.replace("\"", "").replace(")","").trim();
            String[] split = stringCondition.split("\\(");
            String condition = split[0];
            String sets = split[1];
            if(condition.contains("UCC")){
                conditions.add(new UCC(Collections.singletonList(sets), sets));
            } else if(condition.contains("FD")){
                String[] split1 = sets.split(",");
                conditions.add(new FD(Arrays.asList(split1[0]), Arrays.asList(split1[1]), split1[0], split1[1]));
            } else if(condition.contains("IND")){
                String[] split1 = sets.split(",");
                conditions.add(new IND(Arrays.asList(split1[0]), Arrays.asList(split1[1]), split1[0], split1[1]));
            } else {
                throw new RuntimeException("Did not recognize condition: " + condition + " from input: " + input);
            }
        }
        return conditions;
    }
}
