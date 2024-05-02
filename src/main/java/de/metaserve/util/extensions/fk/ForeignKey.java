package de.metaserve.util.extensions.fk;

import de.metaserve.util.configuration.InputConfiguration;
import de.metaserve.util.singletons.InputConfigurationSingleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ForeignKey {
    String pk_schema;
    String pk_table;
    List<String> pk_column = new ArrayList<>();

    String fk_schema;
    String fk_table;
    List<String> fk_column = new ArrayList<>();

    public void setFkSchema(String value) {
        this.fk_schema = value.toUpperCase();
    }

    public void setFkTable(String value) {
        this.fk_table = value.toUpperCase();
    }

    public void setFkColumn(String value) {
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
        this.pk_column.add(value.toUpperCase());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForeignKey that = (ForeignKey) o;
        return pk_schema.equals(that.pk_schema) && fk_schema.equals(that.fk_schema)
                && pk_table.equals(that.pk_table) && fk_table.equals(that.fk_table)
                && pk_column.containsAll(that.pk_column) && fk_column.containsAll(that.fk_column);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pk_schema, pk_table, listHashFk(), fk_schema, fk_table, listHashPk());
    }

    private int listHashFk(){
        int hash = 0;
        fk_column.sort(String::compareTo);
        for (String col : fk_column){
            hash+= col.hashCode();
        }
        return hash;
    }

    private int listHashPk(){
        int hash = 0;
        pk_column.sort(String::compareTo);
        for (String col : pk_column){
            hash+= col.hashCode();
        }
        return hash;
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
        return "ForeignKey{" +
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
    }

}
