package de.metanome;

import de.metanome.algorithm_integration.*;
import de.metanome.algorithm_integration.input.RelationalInputGenerator;
import de.metanome.algorithm_integration.results.*;
import de.metanome.algorithms.binder.BINDERFile;
import de.metanome.algorithms.dva.DVA;
import de.metanome.algorithms.hyfd.HyFD;
import de.metanome.algorithms.hyucc.HyUCC;
import de.metanome.backend.result_receiver.ResultCache;
import de.metaserve.util.singletons.InputConfigurationSingleton;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;

public class MetanomeImpl implements Metanome{

	private static MetanomeImpl instance;

	private MetanomeImpl(){}
	public static Metanome getInstance() {
		if(instance == null){
			instance = new MetanomeImpl();
		}
		return instance;
	}

	@Override
	public List<Result> executeUCC(String... names) {
		return executeHyUCC(names);
	}

	@Override
	public List<Result> executeIND(String... names) {
		return executeBinder(names);
	}

	@Override
	public List<Result> executesIND(String... fileNames){
		return executeSawfish(fileNames);
	}

	@Override
	public List<Result> executeCARD(String... names) {
		return executeDVA(names);
	}

	@Override
	public List<Result> executeFD(String... names) {
		return executeHyFD(names);
	}

	private static List<Result> executeDVA(String... names) {
		List<Result> allResults = new ArrayList<>();
		try {
			for (String fileName : names) {
				RelationalInputGenerator input = MetanomeHelper.getInput(fileName);

				ResultCache resultReceiver = new ResultCache("MetanomeMock", MetanomeHelper.getAcceptedColumns(input));

				DVA dva = MetanomeHelper.createDva(input, resultReceiver);
				long time = executeDva(dva);

				List<Result> tempResults = resultReceiver.fetchNewResults();

				if (InputConfigurationSingleton.get().getWRITE_RESULTS()) {
					MetanomeHelper.writeResultsToFile(DependencyType.CARD, dva.toString(), fileName, time, tempResults);
				}
				allResults.addAll(tempResults);
			}
		}
		catch (AlgorithmExecutionException | IOException e) {
			e.printStackTrace();
		}
		return allResults;
	}

	private static long executeDva(DVA dva) throws AlgorithmExecutionException {
		//@TODO Remove all Sysos out of DVA or hide behind logger
		PrintStream originalStream = System.out;
		PrintStream dummyStream = new PrintStream(new OutputStream() {
			public void write(int b) {}
		});

		System.setOut(dummyStream);
		long time = System.currentTimeMillis();
		dva.execute();
		time = System.currentTimeMillis() - time;
		System.setOut(originalStream);

		return time;
	}

	public static List<Result> executeHyUCC(String... names) {
		List<Result> allResults = new ArrayList<>();
		try {
			for (String fileName : names) {
				RelationalInputGenerator input = MetanomeHelper.getInput(fileName);
				ResultCache resultReceiver = new ResultCache("MetanomeMock", MetanomeHelper.getAcceptedColumns(input));

				HyUCC hyUCC = MetanomeHelper.createHyUCC(input, resultReceiver);

				long time = System.currentTimeMillis();
				hyUCC.execute();
				time = System.currentTimeMillis() - time;

				List<Result> tempResults = resultReceiver.fetchNewResults();

				if (InputConfigurationSingleton.get().getWRITE_RESULTS()) {
					MetanomeHelper.writeResultsToFile(DependencyType.UCC, hyUCC.toString(), fileName, time, tempResults);
				}
				allResults.addAll(tempResults);
			}
		}
		catch (AlgorithmExecutionException | IOException e) {
			e.printStackTrace();
		}
		return allResults;
	}

	public static List<Result> executeSawfish(String... names) {
		return null;
		/*
		try {

			RelationalInputGenerator[] inputs = new RelationalInputGenerator[names.length];
			List<ColumnIdentifier> columnIdentifiers = new ArrayList<>();
			for (int i = 0; i < names.length; i++) {
				inputs[i] =  MetanomeHelper.getInput(names[i]);
				columnIdentifiers.addAll(MetanomeHelper.getAcceptedColumns(inputs[i]));
			}

			ResultCache resultReceiver = new ResultCache("MetanomeMock", columnIdentifiers);
			//ResultReceiver resultReceiver = new ResultCounter("MetanomeMock", getAcceptedColumns(relationalInputGenerator));


			SawfishInterface sawfish = new SawfishInterface();
			int editDistanceThreshold = 1;
			sawfish.setRelationalInputConfigurationValue(SawfishInterface.Identifier.INPUT_FILES.name(), inputs);
			// Sawfish configuration - see readme for detailed explanation of each value
			sawfish.setIntegerConfigurationValue(SawfishInterface.Identifier.editDistanceThreshold.name(), editDistanceThreshold);
				//sawfish.setStringConfigurationValue(SawfishInterface.Identifier.similarityThreshold.name(), "0.4");
			sawfish.setBooleanConfigurationValue(SawfishInterface.Identifier.tokenMode.name(), false);
			sawfish.setBooleanConfigurationValue(SawfishInterface.Identifier.ignoreShortStrings.name(), false);
			sawfish.setBooleanConfigurationValue(SawfishInterface.Identifier.measureTime.name(), false);
			sawfish.setBooleanConfigurationValue(SawfishInterface.Identifier.ignoreNumericColumns.name(), false);
				//sawfish.setBooleanConfigurationValue(SawfishInterface.Identifier.hybridMode.name(), true);
			sawfish.setResultReceiver(resultReceiver);
			sawfish.setTempFileGenerator(new TempFileGenerator());

			PrintStream originalStream = System.out;
			PrintStream dummyStream = new PrintStream(new OutputStream() {
				public void write(int b) {}
			});

			System.setOut(dummyStream);
			long time = System.currentTimeMillis();
			sawfish.execute();
			time = System.currentTimeMillis() - time;
			System.setOut(originalStream);

			for (int i = 0; i < names.length; i++) {
				List<Result> results = resultReceiver.fetchNewResults();
				if (InputConfigurationSingleton.get().getWRITE_RESULTS()) {
					MetanomeHelper.writeResultsToFile(DependencyType.IND, sawfish.toString(), names[i], time, results);
				}
				return results;
			}
		}
		catch (AlgorithmExecutionException | IOException e) {
			e.printStackTrace();
		}
		return null;

		 */
	}

	//@TODO Refactor like the other methods when left, right problem is solved
	public static List<Result> executeBinder(String... names) {
		try {
			BINDERFile binder = new BINDERFile();

			RelationalInputGenerator[] inputs = new RelationalInputGenerator[names.length];
			List<ColumnIdentifier> columnIdentifiers = new ArrayList<>();
			for (int i = 0; i < names.length; i++) {
				inputs[i] =  MetanomeHelper.getInput(names[i]);
				columnIdentifiers.addAll(MetanomeHelper.getAcceptedColumns(inputs[i]));
			}

			ResultCache resultReceiver = new ResultCache("MetanomeMock", columnIdentifiers);
			//ResultReceiver resultReceiver = new ResultCounter("MetanomeMock", getAcceptedColumns(relationalInputGenerator));


			binder.setRelationalInputConfigurationValue(BINDERFile.Identifier.INPUT_FILES.name(), inputs);
			binder.setBooleanConfigurationValue(BINDERFile.Identifier.DETECT_NARY.name(), InputConfigurationSingleton.get().getNARY());
			binder.setIntegerConfigurationValue(BINDERFile.Identifier.MAX_NARY_LEVEL.name(), InputConfigurationSingleton.get().getMAX_SEARCH_SPACE_LEVEL());
			binder.setIntegerConfigurationValue(BINDERFile.Identifier.INPUT_ROW_LIMIT.name(), InputConfigurationSingleton.get().getFILE_MAX_ROWS());
			binder.setBooleanConfigurationValue(BINDERFile.Identifier.DETECT_NARY.name(), true);//@TODO IDIOT
			binder.setResultReceiver(resultReceiver);

			long time = System.currentTimeMillis();
			binder.execute();
			time = System.currentTimeMillis() - time;

			for (int i = 0; i < names.length; i++) {
				List<Result> results = resultReceiver.fetchNewResults();
				if (InputConfigurationSingleton.get().getWRITE_RESULTS()) {
					MetanomeHelper.writeResultsToFile(DependencyType.IND, binder.toString(), names[i], time, results);
				}
				return results;
			}
		}
		catch (AlgorithmExecutionException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static List<Result> executeHyFD(String... names) {
		List<Result> allResults = new ArrayList<>();
		try {
			for (String fileName : names) {
				RelationalInputGenerator input = MetanomeHelper.getInput(fileName);
				ResultCache resultReceiver = new ResultCache("MetanomeMock", MetanomeHelper.getAcceptedColumns(input));

				HyFD hyFD = MetanomeHelper.createHyFD(input, resultReceiver);

				long time = System.currentTimeMillis();
				hyFD.execute();
				time = System.currentTimeMillis() - time;

				List<Result> results = resultReceiver.fetchNewResults();
				if (InputConfigurationSingleton.get().getWRITE_RESULTS()) {
					MetanomeHelper.writeResultsToFile(DependencyType.FD, hyFD.toString(), fileName, time, results);
				}
				allResults.addAll(results);
			}
		} catch (AlgorithmExecutionException | IOException e) {
			e.printStackTrace();
		}
		return allResults;
	}
}
