package de.metaserve.parser;

import de.metaserve.model.query.Query;
import de.metaserve.util.configuration.ParserConfiguration;
import de.metaserve.util.exceptions.ParseException;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;

public interface Parser {

    static Parser get(ParserConfiguration parserConfig) {
        switch (parserConfig.getParser()){
            case ANTLR:
            default:
                return new ANTLRParser(parserConfig);

        }
    }

    /**
     * Parses the given query string into a Query object.
     *
     * @param queryString the query string to be parsed
     * @return the parsed Query object
     * @throws ParseException if the query string is invalid
     */
    Query parse(String queryString) throws ParseException;

    /**
     * Validates the syntax of the given query string without parsing it.
     * Throws a QueryValidationException if the syntax is invalid.
     *
     * @param queryString the query string to be validated
     * @return the parsed tree from the queryString
     * @throws ParseException if the syntax of the query string is invalid
     */
    ParseTree validate(String queryString) throws ParseException;

    /**
     * Returns the ParserConfiguration used by this parser.
     *
     * @return the ParserConfiguration used by this parser
     */
    ParserConfiguration getConfig();

    /**
     * Closes the parser and releases any resources associated with it.
     *
     * @throws IOException if an I/O error occurs while closing the parser
     */
    void close() throws IOException;

}
