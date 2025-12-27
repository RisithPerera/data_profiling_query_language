package de.metathesis;


import de.metanome.MetanomeHelper;
import de.metanome.algorithm_integration.AlgorithmConfigurationException;
import de.metanome.algorithm_integration.input.RelationalInput;
import de.metanome.algorithm_integration.input.RelationalInputGenerator;
import de.metanome.backend.input.file.FileIterator;
import de.metaserve.util.singletons.InputConfigurationSingleton;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public final class Preprocessor {

    private static final Preprocessor INSTANCE = new Preprocessor();
    private final List<String> tables = new ArrayList<>();

    private int readCount = 0;

    private Preprocessor() {}

    public static Preprocessor getInstance() {
        return INSTANCE;
    }

    // Simulates reading a CSV and returning a Relation
    public void loadRelation(String fileName) {
        if(!tables.contains(fileName)) {
            System.out.println("Loading Relation " + fileName);

            try {Thread.sleep(5000);} catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            tables.add(fileName);
            System.out.println("Finished Loading Relation " + fileName);
        }else{
            try {Thread.sleep(1000);} catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }


       /* String filePath = InputConfigurationSingleton.get().getFileInputPath(fileName);
        File file = InputConfigurationSingleton.get().getin
        RelationalInput relationalInput = new FileIterator(getInputFile().getName(), new FileReader(getInputFile(), charset), setting);
        readCount++;*/
    }

    public int getReadCount() {
        return readCount;
    }
}