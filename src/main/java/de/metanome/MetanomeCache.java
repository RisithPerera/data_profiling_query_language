package de.metanome;

import de.metanome.algorithm_integration.results.*;
import de.metaserve.util.singletons.InputConfigurationSingleton;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MetanomeCache implements Metanome {

    private static MetanomeCache instance;

    private MetanomeCache(){}

    public static Metanome getInstance() {
        if(instance == null)
            instance = new MetanomeCache();
        return instance;
    }

    @Override
    public List<Result> executeUCC(String... fileNames) {
        return readDependenciesFromFiles(DependencyType.UCC, fileNames);
    }

    @Override
    public List<Result> executeFD(String... fileNames) {
        return readDependenciesFromFiles(DependencyType.FD, fileNames);
    }

    @Override
    public List<Result> executeIND(String... fileNames) {
        return readDependenciesFromFiles(DependencyType.IND, fileNames);
    }

    @Override
    public List<Result> executeCARD(String... fileNames) {
        return readDependenciesFromFiles(DependencyType.CARD, fileNames);
    }

    private List<Result> readDependenciesFromFiles(DependencyType type, String... fileNames){
        List<Result> resultList = new ArrayList<>();
        for (int i = 0; i < fileNames.length; i++) {
            List<String> result = readFile(InputConfigurationSingleton.get().getFileResultPath(fileNames[i], type.toString()));
            for (String dep : result) {
                if(dep.length() < 9)
                    continue;
                resultList.addAll(MetanomeHelper.getResultType(type, dep));
            }
        }
        return resultList;
    }

    private List<String> readFile(String fileLocation){
        List<String> results = new ArrayList<>();
        if(!(new File(fileLocation)).exists()){
            return new ArrayList<>(); //@TODO Should be exception but then INDs do not work because other quickhack
        }
        try (BufferedReader br = new BufferedReader(new FileReader(fileLocation))) {
            String line;
            while ((line = br.readLine()) != null) {
                results.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return results;
    }
}
