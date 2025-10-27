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
import java.util.concurrent.TimeUnit;

public class MetanomeImpl implements Metanome{

	private static MetanomeImpl instance;

	private MetanomeImpl(){}
	public static Metanome getInstance() {
		if(instance == null){
			instance = new MetanomeImpl();
		}
		return instance;
	}

	@FunctionalInterface
	interface AlgoRunnable {
		void run() throws AlgorithmExecutionException;
	}

	private static long timeExecution(AlgoRunnable r) throws AlgorithmExecutionException {
		final long start = System.nanoTime();
		r.run();
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
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
				long time = 0;
				try (SystemSilencer ignored = SystemSilencer.silenceOut()) {
					time = timeExecution(dva::execute);
				}

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

	public static List<Result> executeHyUCC(String... names) {
		List<Result> allResults = new ArrayList<>();
		try {
			for (String fileName : names) {
				RelationalInputGenerator input = MetanomeHelper.getInput(fileName);
				ResultCache resultReceiver = new ResultCache("MetanomeMock", MetanomeHelper.getAcceptedColumns(input));

				HyUCC hyUCC = MetanomeHelper.createHyUCC(input, resultReceiver);

				long time = 0;
				try (SystemSilencer ignored = SystemSilencer.silenceOut()) {
					time = timeExecution(hyUCC::execute);
				}

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

	public static List<Result> executeBinder(String... names) {
		try {
			BINDERFile binder;

			RelationalInputGenerator[] inputs = new RelationalInputGenerator[names.length];
			List<ColumnIdentifier> columnIdentifiers = new ArrayList<>();
			for (int i = 0; i < names.length; i++) {
				inputs[i] =  MetanomeHelper.getInput(names[i]);
				columnIdentifiers.addAll(MetanomeHelper.getAcceptedColumns(inputs[i]));
			}

			ResultCache resultReceiver = new ResultCache("MetanomeMock", columnIdentifiers);

			binder = MetanomeHelper.createBINDER(inputs, resultReceiver);

			long time = 0;
			try (SystemSilencer ignored = SystemSilencer.silenceOut()) {
				time = timeExecution(binder::execute);
			}

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

				long time = 0;
				try (SystemSilencer ignored = SystemSilencer.silenceOut()) {
					time = timeExecution(hyFD::execute);
				}

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


	static final class SystemSilencer implements AutoCloseable {
		private final PrintStream originalOut;
		private final PrintStream originalErr;
		private final boolean silenceOut;
		private final boolean silenceErr;

		private SystemSilencer(boolean silenceOut, boolean silenceErr) {
			this.silenceOut = silenceOut;
			this.silenceErr = silenceErr;
			this.originalOut = System.out;
			this.originalErr = System.err;

			if (silenceOut) {
				System.setOut(new PrintStream(OutputStream.nullOutputStream()));
			}
			if (silenceErr) {
				System.setErr(new PrintStream(OutputStream.nullOutputStream()));
			}
		}

		public static SystemSilencer silenceOut() {
			return new SystemSilencer(true, false);
		}

		public static SystemSilencer silenceOutAndErr() {
			return new SystemSilencer(true, true);
		}

		@Override
		public void close() {
			if (silenceOut) {
				System.setOut(originalOut);
			}
			if (silenceErr) {
				System.setErr(originalErr);
			}
		}
	}
}
