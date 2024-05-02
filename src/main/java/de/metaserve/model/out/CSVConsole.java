package de.metaserve.model.out;

import de.metaserve.model.graph.Edge;
import de.metaserve.model.graph.Graph;
import de.metaserve.model.graph.Node;
import de.metaserve.util.singletons.ParserConfigurationSingleton;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class CSVConsole implements Console{
    @Override
    public void print(Graph graph, List<String> selections) {
        System.out.print("Result: ");

        List<List<String>> resultList = new ArrayList<>();
        for (String select : selections) {
            if(!graph.nodes.containsKey(select))
                continue;
            Node selectNode = graph.nodes.get(select);
            List<String> result = selectNode.compress();
            for (Edge edge: selectNode.edges){
                if(edge.isMaxRequired())
                    edge.maximize();
            }
        }

        for (String select : selections) {
            if(!graph.nodes.containsKey(select)) {
                resultList.add(new ArrayList<>());
                continue;
            }
            Node selectNode = graph.nodes.get(select);
            List<String> result = selectNode.compress();
            resultList.add(result);
        }


        List<String> data = new ArrayList<String>();
        for (List<String> innerList : resultList) {
            data.addAll(innerList);
        }


        if(data.isEmpty()) {
            System.out.println("[Empty table]");
            return;
        }
        String fileName = "D:\\metanome\\metaserve\\io\\measurements\\Test\\edit_data\\";//@TODO + ParserConfigurationSingleton.get().fileName;
        try (PrintWriter writer = new PrintWriter(fileName)) {
            int index = 0;
            while(true){
                List<String> row = new ArrayList<>();
                boolean update = false;
                for (int j = 0; j < resultList.size(); j++) {
                    List<String> list = resultList.get(j);
                    if (list.size() <= index){
                        row.add("");
                    } else {
                        row.add(list.get(index));
                        update = true;
                    }
                }
                if(update) {
                    writer.println(listToRecord(row));
                    index++;
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String listToRecord(List<String> row) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < row.size(); i++) {
            sb.append(row.get(i).replace("[", "").replace("]", ""));
            if(i+1 < row.size())
                sb.append(",");
        }
        return sb.toString();
    }
}
