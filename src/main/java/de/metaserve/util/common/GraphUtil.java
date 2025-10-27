package de.metaserve.util.common;

import de.metaserve.executor.min.FDEdge;
import de.metaserve.executor.min.Graph;
import de.metaserve.executor.min.INDEdge;
import de.metaserve.executor.min.UCCEdge;

import java.io.*;
import java.util.*;

public class GraphUtil {

    private static String PATH = "src/test/resources/graphs/";
    public static void saveGraphs(Collection<Graph> graphs, int numberOfEdges) {
        File file = new File(PATH + numberOfEdges);

        try (FileWriter writer = new FileWriter(file)) {
            for (Graph graph : graphs) {
                writer.write(graph.toString() + System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Set<Graph> readGraphs(int numberOfEdges) {
        File file = new File(PATH + numberOfEdges);
        Set<Graph> graphs = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                graphs.add(fromString(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return graphs;
    }

    public static boolean hasGraphFile(int numberOfEdges) {
        return new File(PATH + numberOfEdges).exists() && new File(PATH + numberOfEdges).length() > 0;
    }

    public static Graph fromString(String graphString) {
        graphString = graphString.replace("]","").replace("[","");
        Graph graph = new Graph();
        for (String edge : graphString.split(", ")){
            if (edge.startsWith("UCC")){
                String nodeName = edge.charAt(4) + "";
                graph.addEdge(new UCCEdge(nodeName));
            } else if (edge.startsWith("FD")){
                String left = edge.charAt(3) + "";
                String right = edge.charAt(5) + "";
                graph.addEdge(new FDEdge(left, right));
            } else if (edge.startsWith("IND")){
                String left = edge.charAt(4) + "";
                String right = edge.charAt(6) + "";
                graph.addEdge(new INDEdge(left, right));
            } else {
                throw new RuntimeException("Unkown string format: " + graphString);
            }
        }
        return graph;
    }
}
