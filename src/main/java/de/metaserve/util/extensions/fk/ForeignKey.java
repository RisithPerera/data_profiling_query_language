package de.metaserve.util.extensions.fk;

import de.metanome.algorithm_integration.ColumnIdentifier;
import de.metaserve.util.configuration.InputConfiguration;
import de.metaserve.util.singletons.InputConfigurationSingleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ForeignKey {
    String pk_schema = "";
    String pk_table = "";
    List<String> pk_column = new ArrayList<>();

    String fk_schema = "";
    String fk_table = "";
    List<String> fk_column = new ArrayList<>();

    public void parse(List<ColumnIdentifier> fk, List<ColumnIdentifier> pk) {
        for (ColumnIdentifier id : fk){
            String table = id.getTableIdentifier().replace(".csv", "");
            if (table.contains(".")) {
                String[] schemaTable = table.split("\\.");
                table = schemaTable[1];
                setFkSchema(schemaTable[0]);
            }
            setFkTable(table);
            setFkColumn(id.getColumnIdentifier().replace(".csv", ""));
        }
        for (ColumnIdentifier id : pk){
            String table = id.getTableIdentifier().replace(".csv", "");
            if (table.contains(".")) {
                String[] schemaTable = table.split("\\.");
                table = schemaTable[1];
                setPkSchema(schemaTable[0]);
            }
            setPkTable(table);
            setPkColumn(id.getColumnIdentifier().replace(".csv", ""));
        }
    }

    public static boolean containsBOM(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        // Iterate through each character and check if it's a BOM
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch == '\uFEFF' || ch == '\uFFFE') {
                return true;
            }
        }

        return false;
    }

    public static String removeAllBOMs(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // BOM characters to remove
        char utf8BOM = '\uFEFF';
        char utf16LEBOM = '\uFFFE';

        // Use StringBuilder for efficient removal
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            // Append character only if it's not a BOM
            if (ch != utf8BOM && ch != utf16LEBOM) {
                sb.append(ch);
            }
        }

        return sb.toString();
    }

    public static boolean hasBOM(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        // Check if the first character is the BOM character
        char firstChar = input.charAt(0);
        return firstChar == '\uFEFF' || firstChar == '\uFFFE';
    }

    public void setFkSchema(String value) {
        this.fk_schema = value.toUpperCase();
    }

    public void setFkTable(String value) {
        this.fk_table = value.toUpperCase();
    }

    public void setFkColumn(String value) {
        if (containsBOM(value))
            value = removeAllBOMs(value);
        this.fk_column.add(value.toUpperCase());
    }

    public void setFkColumn(String[] split) {
        for (String v : split) {
            setFkColumn(v);
        }
    }

    public void setPkSchema(String value) {
        this.pk_schema = value.toUpperCase();
    }

    public void setPkTable(String value) {
        this.pk_table = value.toUpperCase();
    }

    public void setPkColumn(String[] split) {
        for (String v : split) {
            setPkColumn(v);
        }
    }
    public void setPkColumn(String value) {
        if (containsBOM(value))
            value = removeAllBOMs(value);
        this.pk_column.add(value.toUpperCase());
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ForeignKey other = (ForeignKey) obj;
        return Objects.equals(pk_schema, other.pk_schema)
                && Objects.equals(pk_table, other.pk_table)
                && Objects.equals(pk_column, other.pk_column)
                && Objects.equals(fk_schema, other.fk_schema)
                && Objects.equals(fk_table, other.fk_table)
                && Objects.equals(fk_column, other.fk_column);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                pk_schema,
                pk_table,
                pk_column != null ? pk_column.hashCode() : 0,
                fk_schema,
                fk_table,
                fk_column != null ? fk_column.hashCode() : 0
        );
    }

    public void parse(String fk, String pk) {
        fk = fk.replaceAll("]","").replaceAll("\\[","").replaceAll(" ", "").replaceAll("\uFEFF", "");
        pk = pk.replaceAll("]","").replaceAll("\\[","").replaceAll(" ", "").replaceAll("\uFEFF", "");
        String[] split_fk = fk.split(",");
        if(split_fk.length > 1){
            for (String e : split_fk) {
                parseFk(e);
            }
        } else {
            parseFk(fk);
        }
        String[] split_pk = pk.split(",");
        if(split_pk.length > 1){
            for (String e : split_pk) {
                parsePk(e);
            }
        } else {
            parsePk(pk);
        }
    }

    private void parsePk(String pk) {
        String[] split_pk = pk.split("\\."+ InputConfigurationSingleton.get().getFILE_ENDING() + "\\.");
        setPkColumn(split_pk[1]);
        split_pk = split_pk[0].split("\\.");
        if(split_pk.length > 1) {
            setPkSchema(split_pk[0]);
            setPkTable(split_pk[1]);
        } else {
            setPkSchema("");
            setPkTable(split_pk[0]);
        }
    }

    private void parseFk(String fk) {
        String[] split_fk = fk.split("\\." + InputConfigurationSingleton.get().getFILE_ENDING() + "\\.");
        setFkColumn(split_fk[1]);
        split_fk = split_fk[0].split("\\.");
        if(split_fk.length > 1) {
            setFkSchema(split_fk[0]);
            setFkTable(split_fk[1]);
        } else {
            setFkSchema("");
            setFkTable(split_fk[0]);
        }
    }

    public double getPKCard(){
        return InputConfiguration.getCard(pk_schema, pk_table, pk_column.get(0));
    }
    public double getFKCard(){
        return InputConfiguration.getCard(fk_schema, fk_table, fk_column.get(0));
    }
    @Override
    public String toString() {
        double pkCard = InputConfiguration.getCard(pk_schema, pk_table, pk_column.get(0));
        double fkCard = InputConfiguration.getCard(fk_schema, fk_table, fk_column.get(0));
        /*return "ForeignKey{" +
                "diff='" + (fkCard/pkCard) + '\'' +
                ", pk_schema='" + pk_schema + '\'' +
                ", pk_table='" + pk_table + '\'' +
                ", pk_column='" + pk_column + '\'' +
                ", card='" + pkCard + '\'' +
                ", fk_schema='" + fk_schema + '\'' +
                ", fk_table='" + fk_table + '\'' +
                ", fk_column='" + fk_column + '\'' +
                ", card='" + fkCard + '\'' +
                '}';

         */
        return "ForeignKey{" +
                ", pk_schema='" + pk_schema + '\'' +
                ", pk_table='" + pk_table + '\'' +
                ", pk_column='" + pk_column + '\'' +
                ", fk_schema='" + fk_schema + '\'' +
                ", fk_table='" + fk_table + '\'' +
                ", fk_column='" + fk_column + '\'' +
                ", hash='" + hashCode() + '\'' +
                '}';
    }

}
