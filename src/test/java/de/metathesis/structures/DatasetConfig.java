package de.metathesis.structures;

public class DatasetConfig {
    private String name;
    private String separator;
    private String quoteChar;
    private boolean hasHeader;
    private String fileEnding;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSeparator() { return separator; }
    public void setSeparator(String separator) { this.separator = separator; }

    public String getQuoteChar() { return quoteChar; }
    public void setQuoteChar(String quoteChar) { this.quoteChar = quoteChar; }

    public boolean isHasHeader() { return hasHeader; }
    public void setHasHeader(boolean hasHeader) { this.hasHeader = hasHeader; }

    public String getFileEnding() { return fileEnding; }
    public void setFileEnding(String fileEnding) { this.fileEnding = fileEnding; }
}