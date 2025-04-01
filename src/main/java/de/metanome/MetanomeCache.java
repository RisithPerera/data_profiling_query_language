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
 /*
    @Override
    public List<Result> executeUCC(String... names) {
        List<Result> inds = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            Config conf = new Config();
            conf.inputDatasetName = names[i];
            List<String> result = readFile(conf, "UCC");
            for (String dep : result) {
                if(dep.length() < 9)
                    continue;
                ColumnCombination results = new ColumnCombination();
                results.setColumnIdentifiers(parseCombination(dep));
                inds.add(new UniqueColumnCombination(results));
            }
        }
        return inds;
    }

    @Override
    public List<Result> executeFD(String... names) {
        List<Result> inds = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            Config conf = new Config();
            conf.inputDatasetName = names[i];
            List<String> result = readFile(conf, "FD");
            for (String dep : result) {
                if(dep.length() < 9)
                    continue;
                String[] split = dep.split(" --> ");
                ColumnCombination left = new ColumnCombination();
                left.setColumnIdentifiers(parseCombination(split[0]));
                for(String right : split[1].split(",")){
                    ColumnIdentifier rightID = parseIdentifier(right.trim());
                    inds.add(new FunctionalDependency(left, rightID));
                }
            }
        }
        return inds;
    }

    @Override
    public List<Result> executeIND(String... names) {
        List<Result> inds = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            Config conf = new Config();
            conf.inputDatasetName = names[i];
            List<String> result = readFile(conf, "IND");
            for (String dep : result) {
                if(dep.length() < 9)
                    continue;
                String[] split = dep.split(" --> ");
                ColumnPermutation right = new ColumnPermutation();
                right.setColumnIdentifiers(parsePermutation(split[0]));
                List<List<ColumnIdentifier>> permutations = parsePermutations(split[1]);
                for (List<ColumnIdentifier> p : permutations) {
                    ColumnPermutation left = new ColumnPermutation();
                    left.setColumnIdentifiers(p);
                    ColumnPermutation rightClone = new ColumnPermutation();
                    rightClone.setColumnIdentifiers(new ArrayList<>(right.getColumnIdentifiers()));
                    inds.add(new InclusionDependency(left, rightClone));
                }
            }
        }
        return inds;
    }

    @Override
    public List<Result> executeCARD(String... names) {
        List<Result> inds = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            Config conf = new Config();
            conf.inputDatasetName = names[i];
            List<String> result = readFile(conf, "CARD");
            for (String dep : result) {
                if(dep.length() < 9)
                    continue;
                String name = dep.substring(dep.indexOf("[[") + 2, dep.indexOf("]]"));
                String values = dep.substring(dep.indexOf("=") + 1, dep.indexOf("}"));
                BasicStatistic bs = new BasicStatistic(parseIdentifier(name));
                bs.addStatistic("Number of Distinct Values", new BasicStatisticValueLong(Long.parseLong(values)));
                inds.add(bs);
            }
        }
        return inds;
    }
            */


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
    public List<Result> executesIND(String... fileNames) {
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
