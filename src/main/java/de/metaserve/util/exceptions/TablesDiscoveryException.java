package de.metaserve.util.exceptions;

public class TablesDiscoveryException extends DPQLException {
    public enum Reason {
        INPUT_FOLDER_MISSING,
        NO_TABLES_FOUND
    }

    private final Reason reason;
    private final String path; // may be null

    public TablesDiscoveryException(Reason reason, String message) {
        super(message);
        this.reason = reason;
        this.path = null;
    }

    public TablesDiscoveryException(Reason reason, String message, String path) {
        super(message);
        this.reason = reason;
        this.path = path;
    }

    public Reason getReason() { return reason; }
    public String getPath()   { return path;   }
}
