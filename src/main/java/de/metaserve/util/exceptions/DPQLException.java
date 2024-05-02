package de.metaserve.util.exceptions;

public class DPQLException extends RuntimeException {
    public DPQLException(String s) {
        super(s);
    }
    public DPQLException(String s, Exception e) {
        super(s, e);
    }
}
