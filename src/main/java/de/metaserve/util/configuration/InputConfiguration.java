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
import de.metanome.util.ExtendedConfigurationSettingFileInput;
import de.metanome.util.ExtendedDefaultFileInputGenerator;
import de.metaserve.util.CardMap;
import de.metaserve.util.singletons.EngineConfigurationSingleton;
import de.metaserve.util.singletons.InputConfigurationSingleton;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
	private Charset FILE_CHAR_SET = Charset.defaultCharset();
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
		//loadDataSetSettings(); //@TODO BROKEN? File seems to be overwritten sometime ago
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
		return new ExtendedDefaultFileInputGenerator(new ExtendedConfigurationSettingFileInput(
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
				FILE_NULL_STRING,
				FILE_CHAR_SET
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

	public String getIO_FOLDER() {
		return IO_FOLDER;
	}

	public void setIO_FOLDER(String IO_FOLDER) {
		this.IO_FOLDER = IO_FOLDER;
	}

	public String getDATA_FOLDER() {
		return DATA_FOLDER;
	}

	public void setDATA_FOLDER(String DATA_FOLDER) {
		this.DATA_FOLDER = DATA_FOLDER;
	}

	public String getDATA_SET() {
		return DATA_SET;
	}

	public String getRESULT_FOLDER() {
		return RESULT_FOLDER;
	}

	public void setRESULT_FOLDER(String RESULT_FOLDER) {
		this.RESULT_FOLDER = RESULT_FOLDER;
	}

	public String getFILE_ENDING() {
		return FILE_ENDING;
	}

	public void setFILE_ENDING(String FILE_ENDING) {
		this.FILE_ENDING = FILE_ENDING;
	}

	public String getFILE_VALUE_SEPARATOR() {
		return FILE_VALUE_SEPARATOR;
	}

	public void setFILE_VALUE_SEPARATOR(String FILE_VALUE_SEPARATOR) {
		this.FILE_VALUE_SEPARATOR = FILE_VALUE_SEPARATOR;
	}

	public String getFILE_QUOTE_CHAR() {
		return FILE_QUOTE_CHAR;
	}

	public void setFILE_QUOTE_CHAR(String FILE_QUOTE_CHAR) {
		this.FILE_QUOTE_CHAR = FILE_QUOTE_CHAR;
	}

	public String getFILE_ESCAPE() {
		return FILE_ESCAPE;
	}

	public void setFILE_ESCAPE(String FILE_ESCAPE) {
		this.FILE_ESCAPE = FILE_ESCAPE;
	}

	public Integer getFILE_SKIP_LINES() {
		return FILE_SKIP_LINES;
	}

	public void setFILE_SKIP_LINES(Integer FILE_SKIP_LINES) {
		this.FILE_SKIP_LINES = FILE_SKIP_LINES;
	}

	public Boolean getFILE_STRICT_QUOTES() {
		return FILE_STRICT_QUOTES;
	}

	public void setFILE_STRICT_QUOTES(Boolean FILE_STRICT_QUOTES) {
		this.FILE_STRICT_QUOTES = FILE_STRICT_QUOTES;
	}

	public Boolean getFILE_IGNORE_LEADING_WHITESPACE() {
		return FILE_IGNORE_LEADING_WHITESPACE;
	}

	public void setFILE_IGNORE_LEADING_WHITESPACE(Boolean FILE_IGNORE_LEADING_WHITESPACE) {
		this.FILE_IGNORE_LEADING_WHITESPACE = FILE_IGNORE_LEADING_WHITESPACE;
	}

	public Boolean getFILE_HAS_HEADER() {
		return FILE_HAS_HEADER;
	}

	public void setFILE_HAS_HEADER(Boolean FILE_HAS_HEADER) {
		this.FILE_HAS_HEADER = FILE_HAS_HEADER;
	}

	public Boolean getFILE_SKIP_DIFFERING_LINES() {
		return FILE_SKIP_DIFFERING_LINES;
	}

	public void setFILE_SKIP_DIFFERING_LINES(Boolean FILE_SKIP_DIFFERING_LINES) {
		this.FILE_SKIP_DIFFERING_LINES = FILE_SKIP_DIFFERING_LINES;
	}

	public String getFILE_NULL_STRING() {
		return FILE_NULL_STRING;
	}

	public void setFILE_NULL_STRING(String FILE_NULL_STRING) {
		this.FILE_NULL_STRING = FILE_NULL_STRING;
	}

	public Boolean getFILE_NULL_EQUALS_NULL() {
		return FILE_NULL_EQUALS_NULL;
	}

	public void setFILE_NULL_EQUALS_NULL(Boolean FILE_NULL_EQUALS_NULL) {
		this.FILE_NULL_EQUALS_NULL = FILE_NULL_EQUALS_NULL;
	}

	public Integer getFILE_MAX_ROWS() {
		return FILE_MAX_ROWS;
	}

	public void setFILE_MAX_ROWS(Integer FILE_MAX_ROWS) {
		this.FILE_MAX_ROWS = FILE_MAX_ROWS;
	}

	public Charset getFILE_CHAR_SET() {
		return FILE_CHAR_SET;
	}

	public void setFILE_CHAR_SET(Charset FILE_CHAR_SET) {
		this.FILE_CHAR_SET = FILE_CHAR_SET;
	}

	public String getFILE_STATISTIC_NAME() {
		return FILE_STATISTIC_NAME;
	}

	public void setFILE_STATISTIC_NAME(String FILE_STATISTIC_NAME) {
		this.FILE_STATISTIC_NAME = FILE_STATISTIC_NAME;
	}

	public String getFILE_RESULT_NAME() {
		return FILE_RESULT_NAME;
	}

	public void setFILE_RESULT_NAME(String FILE_RESULT_NAME) {
		this.FILE_RESULT_NAME = FILE_RESULT_NAME;
	}

	public Boolean getWRITE_RESULTS() {
		return WRITE_RESULTS;
	}

	public void setWRITE_RESULTS(Boolean WRITE_RESULTS) {
		this.WRITE_RESULTS = WRITE_RESULTS;
	}

	public Boolean getVALIDATE_PARALLEL() {
		return VALIDATE_PARALLEL;
	}

	public void setVALIDATE_PARALLEL(Boolean VALIDATE_PARALLEL) {
		this.VALIDATE_PARALLEL = VALIDATE_PARALLEL;
	}

	public Boolean getENABLE_MEMORY_GUARDIAN() {
		return ENABLE_MEMORY_GUARDIAN;
	}

	public void setENABLE_MEMORY_GUARDIAN(Boolean ENABLE_MEMORY_GUARDIAN) {
		this.ENABLE_MEMORY_GUARDIAN = ENABLE_MEMORY_GUARDIAN;
	}

	public Integer getMAX_SEARCH_SPACE_LEVEL() {
		return MAX_SEARCH_SPACE_LEVEL;
	}

	public void setMAX_SEARCH_SPACE_LEVEL(Integer MAX_SEARCH_SPACE_LEVEL) {
		this.MAX_SEARCH_SPACE_LEVEL = MAX_SEARCH_SPACE_LEVEL;
	}

	public Integer getMIN_SEARCH_SPACE_LEVEL() {
		return MIN_SEARCH_SPACE_LEVEL;
	}

	public void setMIN_SEARCH_SPACE_LEVEL(Integer MIN_SEARCH_SPACE_LEVEL) {
		this.MIN_SEARCH_SPACE_LEVEL = MIN_SEARCH_SPACE_LEVEL;
	}

	public Boolean getNARY() {
		return NARY;
	}

	public void setNARY(Boolean NARY) {
		this.NARY = NARY;
	}

	public HashMap<String, DataSet> getDataSetHashMap() {
		return dataSetHashMap;
	}

	public void setDataSetHashMap(HashMap<String, DataSet> dataSetHashMap) {
		this.dataSetHashMap = dataSetHashMap;
	}

	public static HashMap<String, Long> getCardMap() {
		return cardMap;
	}

	public static void setCardMap(HashMap<String, Long> cardMap) {
		InputConfiguration.cardMap = cardMap;
	}

	public static int getMaxCard() {
		return maxCard;
	}

	public static void setMaxCard(int maxCard) {
		InputConfiguration.maxCard = maxCard;
	}

	public static int getMinCard() {
		return minCard;
	}

	public static void setMinCard(int minCard) {
		InputConfiguration.minCard = minCard;
	}

	public static String getPath() {
		return path;
	}

	public static void setPath(String path) {
		InputConfiguration.path = path;
	}
}
