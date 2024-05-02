package de.metaserve.model.out;

import de.metaserve.model.graph.Edge;
import de.metaserve.model.graph.Graph;
import de.metaserve.model.graph.Node;

import java.util.*;

public class DefaultConsole implements Console {

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
        System.out.println(selections);
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
                System.out.println(row);
                index++;
            } else {
                break;
            }
        }
        System.out.println("#" + index + " Records");
    }
}
