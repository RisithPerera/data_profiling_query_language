package de.metaserve.util.configuration;

import lombok.Data;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Data
public class DataSet {
    String DATA_SET = "TPCH";
    String FILE_ENDING = "csv";
    String FILE_VALUE_SEPARATOR = ";";
    String FILE_QUOTE_CHAR = "\"";
    String FILE_ESCAPE = "\\";
    Integer FILE_SKIP_LINES = 0;
    Boolean FILE_STRICT_QUOTES = false;
    Boolean FILE_IGNORE_LEADING_WHITESPACE = true;
    Boolean FILE_HAS_HEADER = true;
    Boolean FILE_SKIP_DIFFERING_LINES = true;
    String FILE_NULL_STRING = "";
    Charset FILE_CHAR_SET = StandardCharsets.UTF_8;

    public DataSet(String[] data){
        String DATA_SET = data[0];
        String FILE_ENDING = data[1];
        String FILE_VALUE_SEPARATOR = data[2];
        String FILE_QUOTE_CHAR = data[3];
        String FILE_ESCAPE = data[4];
        Integer FILE_SKIP_LINES = Integer.parseInt(data[5]);
        Boolean FILE_STRICT_QUOTES = Boolean.parseBoolean(data[6]);
        Boolean FILE_IGNORE_LEADING_WHITESPACE = Boolean.parseBoolean(data[7]);
        Boolean FILE_HAS_HEADER = Boolean.parseBoolean(data[8]);
        Boolean FILE_SKIP_DIFFERING_LINES = Boolean.parseBoolean(data[9]);
        String FILE_NULL_STRING = data[10];
        Charset FILE_CHAR_SET = Charset.forName(data[11]);
    }
}
