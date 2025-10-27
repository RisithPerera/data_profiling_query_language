package de.metaserve.util.listener;

import java.util.List;

public interface Listenable<E extends Listener> {

    /**
     * Adds a query execution listener to the query engine. The listener will be notified of query execution events.
     * @param listener The query execution listener to add.
     */
    default void addListener(E listener){
        getListeners().add(listener);
    }

    /**
     * Removes a query execution listener from the query engine. The listener will no longer be notified of query execution events.
     * @param listener The query execution listener to remove.
     */
    default void removeListener(E listener){
        getListeners().remove(listener);
    }

    /**
     * Gets all current Listeners.
     * @return A list of all current Listeners.
     */
    List<E> getListeners();
}
