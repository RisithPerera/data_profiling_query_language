package de.metathesis.structures;

import de.vandermeer.asciitable.AT_Row;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestLine;
import de.vandermeer.asciithemes.u8.U8_Grids;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import lombok.Getter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ResultTable {
    private final List<String> columnNames;
    private final List<List<List<String>>> rows = new ArrayList<>();

    public ResultTable(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    public void addRow(List<List<String>> row) {
        this.rows.add(row);
    }

    public int size(){
       return this.rows.size();
    }

    @Override
    public String toString() {
        return toCsv();
    }

    public void toFile(String path) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(toCsv());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write ResultTable to file: " + path, e);
        }
    }

    private String toCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(", ", this.columnNames)).append("\n");

        // Rows
        for (List<List<String>> row : this.rows) {
            List<String> displayRow = new ArrayList<>();
            for (List<String> cell : row) {
                displayRow.add("[" + String.join(", ", cell) + "]");
            }
            sb.append(String.join(", ", displayRow));
            sb.append("\n");
        }

        return sb.toString();
    }

    private String toAsciiTable() {
        AsciiTable asciiTable = new AsciiTable();
        asciiTable.addRule();

        // Header — centered
        AT_Row headerRow = asciiTable.addRow(this.columnNames);
        headerRow.setTextAlignment(TextAlignment.CENTER);
        asciiTable.addRule();

        // Rows — left aligned
        for (List<List<String>> row : this.rows) {
            List<String> displayRow = new ArrayList<>();

            for (List<String> cell : row) {
                displayRow.add("[" + String.join(", ", cell) + "]");
            }

            AT_Row dataRow = asciiTable.addRow(displayRow);
            dataRow.setTextAlignment(TextAlignment.LEFT);
            asciiTable.addRule();
        }

        asciiTable.getContext().setGrid(U8_Grids.borderDouble());

        // Auto width based on content
        asciiTable.getRenderer().setCWC(new CWC_LongestLine());
        return asciiTable.render();
    }
}