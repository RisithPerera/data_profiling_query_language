package de.metaserve.parser;

import de.metaserve.DPQLLexer;
import de.metaserve.DPQLParser;
import de.metaserve.model.listener.Listenable;
import de.metaserve.model.listener.ParserListener;
import de.metaserve.parser.query.Query;
import de.metaserve.util.configuration.ParserConfiguration;
import de.metaserve.util.exceptions.ParseException;
import de.metaserve.util.singletons.EngineConfigurationSingleton;
import de.metaserve.util.singletons.ParserConfigurationSingleton;
import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ANTLRParser implements Parser, Listenable<ParserListener> {
    private DPQLParser parser;
    private final ParserConfiguration configuration;
    private final List<ParserListener> listeners;

    public ANTLRParser(ParserConfiguration configuration) {
        this.configuration = configuration;
        this.listeners = new ArrayList<>();
    }

    public ANTLRParser() {
        this(ParserConfigurationSingleton.get());
    }

    @Override
    public Query parse(String queryString) throws ParseException {
        queryString = queryString.toUpperCase();

        ParseTree parseTree = validate(queryString);

        Query query = getQuery(parseTree);

        //query.parse(); @TODO Executor should execute the query
        return query;
    }

    @Override
    public ParseTree validate(String queryString) throws ParseException {
        return getParseTree(queryString);
    }

    @Override
    public ParserConfiguration getConfig() {
        return configuration;
    }

    @Override
    public void close() {

    }

    @Override
    public List<ParserListener> getListeners() {
        return listeners;
    }

    private Query getQuery(ParseTree parseTree) {
        ParseTreeWalker walker = new ParseTreeWalker();
        CustomDPQLParseListener listener = new CustomDPQLParseListener(parser.getRuleNames());
        walker.walk(listener, parseTree);
        return listener.build();
    }

    private ParseTree getParseTree(String queryString) throws ParseException {
        DPQLLexer lexer = new DPQLLexer(CharStreams.fromString(queryString));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        parser = new DPQLParser(tokens);

        // pretty listener
        parser.removeErrorListeners();
        parser.addErrorListener(new PrettyErrorListener(queryString));

        ParseTree tree = parser.dpqlStatement();

        if (EngineConfigurationSingleton.get().isLog()) {
            TreeViewer viewer = new TreeViewer(Arrays.asList(parser.getRuleNames()), tree);
            viewer.open();
        }
        return tree;
    }

    private ParseTree getParseTreeOld(String queryString) throws ParseException{
        DPQLLexer lexer = new DPQLLexer(CharStreams.fromString(queryString));
        parser = new DPQLParser(new CommonTokenStream(lexer));
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                if (e instanceof NoViableAltException){
                    String[] errorTokens = msg.split(" ");
                    String errorToken = errorTokens[errorTokens.length-1].replace("'", "");
                    throw new ParseException("Could not recognize '" + errorToken + "'");
                } else {
                    throw new ParseException("Failed to parse at line " + line + " due to " + msg + " with pos " + charPositionInLine + " error symbol " + offendingSymbol, e);
                }
            }
        });

        ParseTree tree = parser.dpqlStatement();

        if(EngineConfigurationSingleton.get().isLog()){
            TreeViewer viewer = new TreeViewer(Arrays.asList(parser.getRuleNames()),tree);
            viewer.open();
        }
        return tree;
    }
}
