package de.metaserve.util.configuration;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import de.metanome.algorithm_integration.AlgorithmConfigurationException;
import de.metanome.algorithm_integration.configuration.ConfigurationSettingFileInput;
import de.metanome.algorithm_integration.input.RelationalInputGenerator;
import de.metanome.algorithm_integration.results.BasicStatistic;
import de.metanome.algorithm_integration.results.Result;
import de.metanome.backend.input.file.DefaultFileInputGenerator;
import de.metanome.Metanome;
import de.metaserve.util.CardMap;
import de.metaserve.util.singletons.EngineConfigurationSingleton;
import de.metaserve.util.singletons.InputConfigurationSingleton;
import lombok.Data;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Data
public class InputConfiguration implements Configuration {
	private String IO_FOLDER = "io";
	private String DATA_FOLDER = "data";
	private String DATA_SET = "TPCH";
	private String RESULT_FOLDER = "results";
	private String FILE_ENDING = "csv";
	private String FILE_VALUE_SEPARATOR = ";";
	private String FILE_QUOTE_CHAR = "\"";
	private String FILE_ESCAPE = "\\";
	private Integer FILE_SKIP_LINES = 0;
	private Boolean FILE_STRICT_QUOTES = false;
	private Boolean FILE_IGNORE_LEADING_WHITESPACE = true;
	private Boolean FILE_HAS_HEADER = true;
	private Boolean FILE_SKIP_DIFFERING_LINES = true;
	private String FILE_NULL_STRING = "";
	private Boolean FILE_NULL_EQUALS_NULL = true;
	private Integer FILE_MAX_ROWS = -1;
	private Charset FILE_CHAR_SET = StandardCharsets.UTF_8;
	private String FILE_STATISTIC_NAME = "statistics.txt";
	private String FILE_RESULT_NAME = "results.txt";
	private Boolean WRITE_RESULTS = true;

	private Boolean VALIDATE_PARALLEL = true;
	private Boolean ENABLE_MEMORY_GUARDIAN = true;
	private Integer MAX_SEARCH_SPACE_LEVEL = -1;
	private Integer MIN_SEARCH_SPACE_LEVEL = -1;
	private Boolean NARY = true;

	private HashMap<String, DataSet> dataSetHashMap = new HashMap<>();

	//@TODO MOVE TO CONDITIONS
	public static HashMap<String, Long> cardMap = new CardMap();
	public static int maxCard = -1;
	public static int minCard = -1;

	public InputConfiguration(){
		try {
			this.load();
			this.loadDataSets();
		} catch (RuntimeException e) {
			throw new RuntimeException("Could not load Input config", e);
		}
	}

	public static long getCard(String name) {
		long card;
		String tableName = name.split("."+ InputConfigurationSingleton.get().getFILE_ENDING())[0];
		if(InputConfiguration.cardMap == null || InputConfiguration.cardMap.get(tableName) == null) {
			boolean cache = EngineConfigurationSingleton.get().isCache();
			EngineConfigurationSingleton.get().setCache(true);
			InputConfigurationSingleton.get().buildCardMap(Arrays.asList(tableName));
			EngineConfigurationSingleton.get().setCache(cache);
		}
		if(InputConfiguration.cardMap.get(name) == null){
			card = InputConfiguration.cardMap.get(name.split(",")[0]);
		} else {
			card = InputConfiguration.cardMap.get(name);
		}
		return card;
	}

	public static long getCard(String schema, String table, String name) {
		if(InputConfiguration.cardMap == null || InputConfiguration.cardMap.get(table) == null) {
			boolean cache = EngineConfigurationSingleton.get().isCache();
			EngineConfigurationSingleton.get().setCache(true);
			InputConfigurationSingleton.get().buildCardMap(Arrays.asList(table));
			EngineConfigurationSingleton.get().setCache(cache);
		}
		String search;
		if(schema.equals("")){
			search = table + "." + InputConfigurationSingleton.get().getFILE_ENDING() + "." + name;
		} else {
			search = schema + "." + table + "." + InputConfigurationSingleton.get().getFILE_ENDING() + "." + name;
		}
		search = search.toLowerCase();
		search.hashCode();
		if(InputConfiguration.cardMap.containsKey(search)){
			return InputConfiguration.cardMap.get(search);
		} else {
			return -1L;
		}
	}

	public static String path = System.getProperty("user.dir");

	public void setDATA_SET(String dataSetName) {
		this.DATA_SET = dataSetName;
		loadDataSetSettings();
	}

	private void loadDataSets() {
		try (CSVReader reader = new CSVReader(new FileReader(path + File.separator + "io" + File.separator + "configurations" + File.separator + "datasetsconfig.csv"))) {
			String[] header = reader.readNext();
			String[] data;
			while ((data = reader.readNext()) != null) {
				if(data.length == 12) dataSetHashMap.put(data[0], new DataSet(data));
				else throw new IOException("Data Sets config file contains errors!");
			}
		} catch (IOException | CsvValidationException e) {
            throw new RuntimeException(e);
        }
    }
	private void loadDataSetSettings() {
		if(dataSetHashMap.containsKey(DATA_SET)) {
			DataSet currentDataSet = dataSetHashMap.get(DATA_SET);
			this.DATA_SET = currentDataSet.DATA_SET;
			this.FILE_ENDING = currentDataSet.FILE_ENDING;
			this.FILE_VALUE_SEPARATOR = currentDataSet.FILE_VALUE_SEPARATOR;
			this.FILE_QUOTE_CHAR = currentDataSet.FILE_QUOTE_CHAR;
			this.FILE_ESCAPE = currentDataSet.FILE_ESCAPE;
			this.FILE_SKIP_LINES = currentDataSet.FILE_SKIP_LINES;
			this.FILE_STRICT_QUOTES = currentDataSet.FILE_STRICT_QUOTES;
			this.FILE_IGNORE_LEADING_WHITESPACE = currentDataSet.FILE_IGNORE_LEADING_WHITESPACE;
			this.FILE_HAS_HEADER = currentDataSet.FILE_HAS_HEADER;
			this.FILE_SKIP_DIFFERING_LINES = currentDataSet.FILE_SKIP_DIFFERING_LINES;
			this.FILE_NULL_STRING = currentDataSet.FILE_NULL_STRING;
		}
	}

	public String getInputPath(){
		return path /*.substring(0, System.getProperty("user.dir").lastIndexOf(File.separator)) */+ File.separator + IO_FOLDER + File.separator + DATA_FOLDER + File.separator + DATA_SET + File.separator;
	}

	public String getOutputPath(){
		return path /*.substring(0, System.getProperty("user.dir").lastIndexOf(File.separator)) */+ File.separator + IO_FOLDER + File.separator + RESULT_FOLDER + File.separator + DATA_SET + File.separator;
	}

	public String getMetaDataPath(){
		return getInputPath() +  "Metadata" + File.separator;
	}

	public String getFileInputPath(String name){
		return getInputPath() + name + "." + FILE_ENDING;
	}

	public String getFileResultPath(String name, String dependency){
		return getInputPath() + name + File.separator + dependency + "_" + FILE_RESULT_NAME;
	}

	public String getFileStatisticPath(String name, String dependency){
		return getInputPath() + name + File.separator + dependency + "_" + FILE_STATISTIC_NAME;
	}

	public RelationalInputGenerator getInputGenerator(String fileName) throws AlgorithmConfigurationException {
		return new DefaultFileInputGenerator(new ConfigurationSettingFileInput(
				getFileInputPath(fileName),
				true,
				FILE_VALUE_SEPARATOR.charAt(0),
				FILE_QUOTE_CHAR.charAt(0),
				FILE_ESCAPE.charAt(0),
				FILE_STRICT_QUOTES,
				FILE_IGNORE_LEADING_WHITESPACE,
				FILE_SKIP_LINES,
				FILE_HAS_HEADER,
				FILE_SKIP_DIFFERING_LINES,
				FILE_NULL_STRING
		));
	}

	@Override
	public Properties saveClassToProperties() {
    	Properties config = loadPropertiesFromFile();
		config.setProperty("IO_FOLDER", IO_FOLDER);
		config.setProperty("DATA_FOLDER", DATA_FOLDER);
		config.setProperty("DATA_SET", DATA_SET);
		config.setProperty("RESULT_FOLDER", RESULT_FOLDER);
		config.setProperty("FILE_ENDING", FILE_ENDING);
		config.setProperty("FILE_VALUE_SEPARATOR", FILE_VALUE_SEPARATOR);
		config.setProperty("FILE_QUOTE_CHAR", FILE_QUOTE_CHAR);
		config.setProperty("FILE_ESCAPE", FILE_ESCAPE);
		config.setProperty("FILE_SKIP_LINES", String.valueOf(FILE_SKIP_LINES));
		config.setProperty("FILE_STRICT_QUOTES", String.valueOf(FILE_STRICT_QUOTES));
		config.setProperty("FILE_IGNORE_LEADING_WHITESPACE", String.valueOf(FILE_IGNORE_LEADING_WHITESPACE));
		config.setProperty("FILE_HAS_HEADER", String.valueOf(FILE_HAS_HEADER));
		config.setProperty("FILE_SKIP_DIFFERING_LINES", String.valueOf(FILE_SKIP_DIFFERING_LINES));
		config.setProperty("FILE_NULL_STRING", FILE_NULL_STRING);
		config.setProperty("FILE_NULL_EQUALS_NULL", String.valueOf(FILE_NULL_EQUALS_NULL));
		config.setProperty("FILE_MAX_ROWS", String.valueOf(FILE_MAX_ROWS));
		config.setProperty("FILE_CHAR_SET", FILE_CHAR_SET.name());
		config.setProperty("FILE_STATISTIC_NAME", FILE_STATISTIC_NAME);
		config.setProperty("FILE_RESULT_NAME", FILE_RESULT_NAME);
		config.setProperty("WRITE_RESULTS", String.valueOf(WRITE_RESULTS));
		config.setProperty("VALIDATE_PARALLEL", String.valueOf(VALIDATE_PARALLEL));
		config.setProperty("ENABLE_MEMORY_GUARDIAN", String.valueOf(ENABLE_MEMORY_GUARDIAN));
		return config;
	}

	@Override
	public void loadClassFromProperties(Properties config) {
		IO_FOLDER = config.getProperty("IO_FOLDER");
		DATA_FOLDER = config.getProperty("DATA_FOLDER");
		DATA_SET = config.getProperty("DATA_SET");
		RESULT_FOLDER = config.getProperty("RESULT_FOLDER");
		FILE_ENDING = config.getProperty("FILE_ENDING");
		FILE_VALUE_SEPARATOR = config.getProperty("FILE_VALUE_SEPARATOR");
		FILE_QUOTE_CHAR = config.getProperty("FILE_QUOTE_CHAR");
		FILE_ESCAPE = config.getProperty("FILE_ESCAPE");
		FILE_SKIP_LINES = Integer.parseInt(config.getProperty("FILE_SKIP_LINES"));
		FILE_STRICT_QUOTES = Boolean.parseBoolean(config.getProperty("FILE_STRICT_QUOTES"));
		FILE_IGNORE_LEADING_WHITESPACE = Boolean.parseBoolean(config.getProperty("FILE_IGNORE_LEADING_WHITESPACE"));
		FILE_HAS_HEADER = Boolean.parseBoolean(config.getProperty("FILE_HAS_HEADER"));
		FILE_SKIP_DIFFERING_LINES = Boolean.parseBoolean(config.getProperty("FILE_SKIP_DIFFERING_LINES"));
		FILE_NULL_STRING = config.getProperty("FILE_NULL_STRING");
		FILE_NULL_EQUALS_NULL = Boolean.parseBoolean(config.getProperty("FILE_NULL_EQUALS_NULL"));
		FILE_MAX_ROWS = Integer.parseInt(config.getProperty("FILE_MAX_ROWS"));
		FILE_CHAR_SET = Charset.forName(config.getProperty("FILE_CHAR_SET"));
		FILE_STATISTIC_NAME = config.getProperty("FILE_STATISTIC_NAME");
		FILE_RESULT_NAME = config.getProperty("FILE_RESULT_NAME");
		WRITE_RESULTS = Boolean.parseBoolean(config.getProperty("WRITE_RESULTS"));
		VALIDATE_PARALLEL = Boolean.parseBoolean(config.getProperty("VALIDATE_PARALLEL"));
		ENABLE_MEMORY_GUARDIAN = Boolean.parseBoolean(config.getProperty("ENABLE_MEMORY_GUARDIAN"));
	}

	public void buildCardMap(List<String> tables) {
		List<Result> cardResults = Metanome.getInstance().executeCARD(tables.toArray(new String[tables.size()]));
		for (Result result : cardResults) {
			BasicStatistic bs = (BasicStatistic) result;
			Long nofv = (Long) bs.getStatisticMap().get("Number of Distinct Values").getValue();
			cardMap.put(bs.getColumnCombination().getColumnIdentifiers().iterator().next().toString(), nofv);
		}
	}
}
