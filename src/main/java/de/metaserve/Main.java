package de.metaserve;

import de.metaserve.engine.Metaserve;
import de.metaserve.engine.QueryEngine;
import de.metaserve.model.listener.QueryExecutionListener;
import de.metaserve.model.query.Query;
import de.metaserve.model.result.ResultSet;
import de.metaserve.util.configuration.InputConfiguration;
import de.metaserve.util.exceptions.DPQLException;
import de.metaserve.util.singletons.EngineConfigurationSingleton;
import de.metaserve.util.cli.DPQLConsoleHighlighter; // NEW
import de.metaserve.util.exceptions.ParseException; // NEW (so you can catch parser errors cleanly)


import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;

public class Main {

	private static final String PROMPT = "metaserve> ";
	private static final DateTimeFormatter TS_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

	public static void main(String[] args) {
		// base configuration
		InputConfiguration inputConfig = EngineConfigurationSingleton.get()
				.setCache(true)
				.getInputConfig();
		inputConfig.setDATA_SET("TPCHNEW");

		printBanner();
		printHelp();

		try (Scanner scanner = new Scanner(System.in)) {

			// Create one engine instance and reuse it
			Metaserve metaserve = new Metaserve();
			metaserve.addListener(new QueryExecutionListener() {
				@Override
				public void onEvent(QueryEngine.QueryState event, Query query) {
					// You could log engine state transitions here if needed.
				}

				@Override
				public void onQueryCompleted(Query query, List<ResultSet> resultSet, long totalTime, int resultSize) {
					log("Query completed in " + totalTime + " ms, rows: " + resultSize);
					if (query != null && query.getMetaData() != null) {
						System.out.println("-- Metadata --");
						System.out.println(query.getMetaData());
						System.out.println();
					}
				}

				@Override
				public void onEngineClosed() {
					log("Engine closed.");
				}

				@Override
				public void onEvent(String event) {
					// Optional: log string events
				}
			});

			// REPL loop
			while (true) {
				System.out.print(PROMPT);
				if (!scanner.hasNextLine()) {
					System.out.println(); // nice newline on EOF
					break;
				}

				String line = scanner.nextLine().trim();
				if (line.isEmpty()) continue;

				// Commands start with ':'
				if (line.startsWith(":")) {
					if (handleCommand(line)) {
						continue; // handled
					} else {
						System.out.println("Unknown command. Type :help for a list of commands.");
						continue;
					}
				}

				// Treat as a query
				try {
					//System.out.println(DPQLConsoleHighlighter.highlight(line));
					Instant start = Instant.now();
					List<ResultSet> resultSetList = metaserve.executeQuery(line);
					Instant end = Instant.now();
					long elapsedMs = Duration.between(start, end).toMillis();

					if (resultSetList == null || resultSetList.isEmpty()) {
						log("No results. (" + elapsedMs + " ms)");
						continue;
					}

					// Print the first ResultSet (and count)
					ResultSet first = resultSetList.get(0);
					System.out.println("-- Result (showing first set of " + resultSetList.size() + ") --");
					System.out.println(Objects.toString(first));
					System.out.println();
					log("Done in " + elapsedMs + " ms.");
				} catch (DPQLException dpex) { // NEW: unwrap ParseException
					ParseException pe = findCause(dpex, ParseException.class);
					if (pe != null) {
						System.err.println("\n" + pe.getMessage()); // already includes caret underline
						continue;
					}
					// Not a parse error; show the DPQLException message
					System.err.println("[ERROR] DPQLException: " + dpex.getMessage());
					// Optional: dpex.printStackTrace();
				} catch (ParseException pe) { // In case some paths throw it directly
					System.err.println("\n" + pe.getMessage());
					continue;
				} catch (Exception ex) {
					System.err.println("[ERROR] " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
					// Optional: ex.printStackTrace();
				}
			}

			// If Metaserve supports explicit close(), you can add:
			// try { metaserve.close(); } catch (Exception ignore) {}
		}
	}

	private static boolean handleCommand(String line) {
		String cmd = line.toLowerCase(Locale.ROOT);

		if (":help".equals(cmd)) {
			printHelp();
			return true;
		}
		if (":quit".equals(cmd) || ":exit".equals(cmd)) {
			log("Goodbye.");
			System.exit(0); // intentional exit from REPL
		}
		if (":cache".equals(cmd)) {
			boolean newValue = !EngineConfigurationSingleton.get().isCache();
			EngineConfigurationSingleton.get().setCache(newValue);
			System.out.println("CACHE is " + (newValue ? "ON" : "OFF"));
			return true;
		}
		if (cmd.startsWith(":dataset")) {
			String[] parts = line.split("\\s+", 2);
			if (parts.length < 2 || parts[1].isBlank()) {
				System.out.println("Usage: :dataset <NAME>");
				return true;
			}
			String name = parts[1].trim();
			EngineConfigurationSingleton.get().getInputConfig().setDATA_SET(name);
			System.out.println("DATA_SET is now '" + name + "'");
			return true;
		}

		return false; // unknown command
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

	private static <T extends Throwable> T findCause(Throwable ex, Class<T> type) {
		Throwable cur = ex;
		while (cur != null) {
			if (type.isInstance(cur)) return type.cast(cur);
			cur = cur.getCause();
		}
		return null;
	}
}
