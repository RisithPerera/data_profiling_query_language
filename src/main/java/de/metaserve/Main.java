package de.metaserve;

import de.metaserve.engine.Metaserve;
import de.metaserve.model.listener.ComplitionListener;
import de.metaserve.model.result.ResultSet;
import de.metaserve.util.configuration.EngineConfiguration;
import de.metaserve.util.configuration.InputConfiguration;
import de.metaserve.util.exceptions.DPQLException;
import de.metaserve.util.exceptions.Exceptions;
import de.metaserve.util.singletons.EngineConfigurationSingleton;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;



public class Main {

	private static final String PROMPT = "metaserve> ";
	private static final DateTimeFormatter TS_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

	public static void main(String[] args) {
		EngineConfiguration cfg = EngineConfigurationSingleton.get();
		InputConfiguration inputConfig = cfg.setCache(true).getInputConfig();
		inputConfig.setDATA_SET("TPCHNEW");

		Runtime.getRuntime().addShutdownHook(new Thread(() -> log("Shutting down...")));

		printBanner();
		printHelp();

		try (Scanner scanner = new Scanner(System.in)) {

			Metaserve metaserve = new Metaserve();
			metaserve.addListener((ComplitionListener) (query, resultSet, totalTime, resultSize) -> {
                log("Query completed in " + totalTime + " ms, rows: " + resultSize);
                if (query != null && query.getMetaData() != null) {
                    System.out.println("-- Metadata --");
                    System.out.println(query.getMetaData());
                    System.out.println();
                }
            });

			// REPL loop
			while (true) {
				System.out.print(PROMPT);
				if (!scanner.hasNextLine()) {
					System.out.println();
					break;
				}

				String line = scanner.nextLine().trim();
				if (line.isEmpty()) continue;

				// Commands start with ':'
				if (line.startsWith(":")) {
					if (handleCommand(line, cfg) == Action.QUIT) break;
					continue;
				}

				try {
					Instant start = Instant.now();
					List<ResultSet> resultSetList = metaserve.executeQuery(line);
					Instant end = Instant.now();
					long elapsedMs = Duration.between(start, end).toMillis();

					if (resultSetList == null || resultSetList.isEmpty()) {
						log("No results. (" + elapsedMs + " ms)");
						continue;
					}

					ResultSet first = resultSetList.get(0);
					System.out.println("-- Result (showing first set of " + resultSetList.size() + ") --");
					System.out.println(first);
					System.out.println();
					log("Done in " + elapsedMs + " ms.");
				} catch (DPQLException dpex) {
					if (Exceptions.handleDpqlException(dpex)) {
						continue;
					}
					System.err.println("[ERROR] DPQLException: " + dpex.getMessage());
				}
			}
		}
	}

	enum Action { CONTINUE, QUIT }

	private static Action handleCommand(String line, EngineConfiguration cfg) {
		String cmd = line.trim();
		switch (cmd.split("\\s+")[0].toLowerCase(Locale.ROOT)) {
			case ":help" -> { printHelp(); return Action.CONTINUE; }
			case ":quit", ":exit" -> { log("Goodbye."); return Action.QUIT; }
			case ":cache" -> {
				boolean newValue = !cfg.isCache();
				cfg.setCache(newValue);
				System.out.println("CACHE is " + (newValue ? "ON" : "OFF"));
				return Action.CONTINUE;
			}
			case ":dataset" -> {
				String[] parts = line.split("\\s+", 2);
				if (parts.length < 2 || parts[1].isBlank()) {
					System.out.println("Usage: :dataset <NAME>");
				} else {
					cfg.getInputConfig().setDATA_SET(parts[1].trim());
					System.out.println("DATA_SET is now '" + parts[1].trim() + "'");
				}
				return Action.CONTINUE;
			}
			default -> {
				System.out.println("Unknown command. Type :help for a list of commands.");
				return Action.CONTINUE;
			}
		}
	}


	private static void printBanner() {
		System.out.println("Metaserve CLI");
		System.out.println("Started at " + LocalDateTime.now().format(TS_FMT));
		System.out.println();
	}

	private static void printHelp() {
		System.out.println("Commands:");
		System.out.println("  :help                    Show this help");
		System.out.println("  :cache                   Toggle cache ON/OFF");
		System.out.println("  :dataset <NAME>          Switch input configuration DATA_SET");
		System.out.println("  :quit | :exit            Quit");
		System.out.println();
		System.out.println("Type any other text to execute it as a query.");
		System.out.println();
	}

	private static void log(String msg) {
		System.out.println("[" + LocalDateTime.now().format(TS_FMT) + "] " + msg);
	}
}
