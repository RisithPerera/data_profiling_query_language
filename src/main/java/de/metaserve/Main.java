package de.metaserve;

import de.metaserve.engine.Metaserve;
import de.metaserve.engine.QueryEngine;
import de.metaserve.model.listener.QueryExecutionListener;
import de.metaserve.model.query.Query;
import de.metaserve.model.result.ResultSet;
import de.metaserve.util.configuration.InputConfiguration;
import de.metaserve.util.singletons.EngineConfigurationSingleton;

import java.util.List;
import java.util.Scanner;

public class Main {
	public static void main(String[] args) {
		InputConfiguration inputConfig = EngineConfigurationSingleton.get().setCache(true).getInputConfig();
		inputConfig.setDATA_SET("TPCHNEW");
		while(true){
			Scanner scanner = new Scanner(System.in);
			System.out.println("Please enter your query:");
			String query = scanner.nextLine();
			if(query.equalsIgnoreCase("CACHE")){
				EngineConfigurationSingleton.get().setCache(!EngineConfigurationSingleton.get().isCache());
				System.out.println("CACHE is " + (EngineConfigurationSingleton.get().isCache() ? "ON" : "OFF"));
				continue;
			}
			Metaserve metaserve = new Metaserve();
			metaserve.addListener(new QueryExecutionListener() {
				@Override
				public void onEvent(QueryEngine.QueryState event, Query query) {}

				@Override
				public void onQueryCompleted(Query query, List<ResultSet> resultSet, long totalTime, int resultSize) {
					System.out.println(query.getMetaData());
				}

				@Override
				public void onEngineClosed() {}

				@Override
				public void onEvent(String event) {}
			});
			List<ResultSet> resultSetList = metaserve.executeQuery(query);
			System.out.println(resultSetList.get(0));
		}
	}

}
