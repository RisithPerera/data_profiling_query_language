package de.metaserve.model.out;

import de.metaserve.model.graph.Graph;
import de.metaserve.util.configuration.ExecutorConfiguration;
import de.metaserve.util.configuration.ParserConfiguration;
import de.metaserve.util.singletons.ExecutorConfigurationSingelton;
import de.metaserve.util.singletons.ParserConfigurationSingleton;

import java.util.List;

public interface Console {

    static Console get() {
        ExecutorConfiguration.Output output = ExecutorConfigurationSingelton.get().getOutputType();
        if(output.equals(ExecutorConfiguration.Output.CONSOLE))
            return new ASCIIConsole();
        else if(output.equals(ExecutorConfiguration.Output.FILE))
            return new CSVConsole();
        if(output.equals(ExecutorConfiguration.Output.PRIMITIVE))
            return new DefaultConsole();
        return new ASCIIConsole();
    }

    void print(Graph graph, List<String> selections);
}
