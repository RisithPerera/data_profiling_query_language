package de.metaserve.util.exceptions;

import org.antlr.v4.runtime.RecognitionException;

public class ParseException extends RuntimeException{
    public ParseException(String s) {
        super(s);
    }

    public ParseException(String s, RecognitionException e) {
        super(s, e);
    }
}
