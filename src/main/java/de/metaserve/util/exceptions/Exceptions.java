package de.metaserve.util.exceptions;

import java.util.Optional;

public class Exceptions {
    static <T extends Throwable> Optional<T> findCause(Throwable ex, Class<T> type) {
        Throwable cur = ex;
        while (cur != null) {
            if (type.isInstance(cur)) return Optional.of(type.cast(cur));
            cur = cur.getCause();
        }
        return Optional.empty();
    }

    public static boolean handleDpqlException(DPQLException dpex) {
        Optional<ParseException> pe = findCause(dpex, ParseException.class);
        if (pe.isPresent()) {
            System.err.println("\n" + pe.get().getMessage());
            return true;
        }

        Optional<TablesDiscoveryException> tde = findCause(dpex, TablesDiscoveryException.class);
        if (tde.isPresent()) {
            TablesDiscoveryException x = tde.get();
            System.err.println("[INPUT] " + x.getReason() + ": " + x.getMessage());
            if (x.getReason() == TablesDiscoveryException.Reason.INPUT_FOLDER_MISSING && x.getPath() != null) {
                System.err.println("Hint: Use :dataset <NAME> or set a valid input path.");
            }
            return true;
        }

        return false;
    }
}
