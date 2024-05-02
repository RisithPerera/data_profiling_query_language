package de.metaserve.parser;

import de.metaserve.DPQLParser;
import de.metaserve.model.query.Query;
import de.metaserve.util.configuration.InputConfiguration;
import de.metaserve.util.singletons.InputConfigurationSingleton;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CustomDPQLParseListener implements ParseTreeListener {

    public static final String SELECT_ELEMENT = "selectelement";
    public static final String FROM_ELEMENT = "fromelement";
    public static final String WHERE_ELEMENT = "whereelement";

    //@TODO parse type of query
    Query query = Query.get();
    String[] rules;

    public CustomDPQLParseListener(String[] ruleNames) {
        this.rules = ruleNames;
    }

    @Override
    public void visitTerminal(TerminalNode node) {

    }

    @Override
    public void visitErrorNode(ErrorNode node) {

    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {

    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
        String rule = rules[ctx.getRuleIndex()];
        switch (rule){
            case SELECT_ELEMENT:
                query.addSelection(getTextFromNode(ctx));
                break;
            case FROM_ELEMENT:
                query.addCC(getRefName(ctx), getTables(ctx));
                break;
            case WHERE_ELEMENT:
                query.addCondition(getTextFromNode(ctx));
                break;
            default:
        }
    }

    //@TODO ParserConfig should give all infos
    //Maybe also verify here or move it to functions in ParserConfig
    private List<String> getTables(ParserRuleContext ctx) {
        List<String> tables = new ArrayList<>();
        for (ParseTree child: ctx.children) {
            if(child instanceof DPQLParser.TableNameContext){
                tables.add(getTextFromNode((ParserRuleContext) child));
            }
        }
        if(tables.contains("*")){
            tables.clear();
            File folder = new File(InputConfigurationSingleton.get().getInputPath());
            if(!folder.exists())
                throw new RuntimeException("Folder " + InputConfigurationSingleton.get().getInputPath() + " not found!");
            for (File fileEntry : Objects.requireNonNull(folder.listFiles())) {
                if(fileEntry.isFile() && fileEntry.getName().endsWith("."+InputConfigurationSingleton.get().getFILE_ENDING())){
                    tables.add(fileEntry.getName().replace("."+InputConfigurationSingleton.get().getFILE_ENDING(), ""));
                }
            }
        }
        return tables;
    }

    private String getRefName(ParserRuleContext ctx) {
        for (ParseTree child: ctx.children) {
            if(child instanceof DPQLParser.IdContext){
                return getTextFromNode((ParserRuleContext) child);
            }
        }
        return null;
    }

    private String getTextFromNode(ParserRuleContext context) {
        int a = context.start.getStartIndex();
        int b = context.stop.getStopIndex();
        CharStream input = context.start.getInputStream();
        Interval interval = new Interval(a,b);
        return input.getText(interval);
    }

    public Query build(){
        return query;
    }
}
