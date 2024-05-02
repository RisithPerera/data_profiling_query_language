package de.metaserve.util;

import java.util.HashMap;

public class CardMap extends HashMap<String, Long> {

    @Override
    public Long get(Object key) {
        return super.get(((String)key).replace("\uFEFF", "").toLowerCase());
    }

    @Override
    public Long put(String key, Long value) {
        return super.put((""+key).replace("\uFEFF", "").toLowerCase(), value);
    }

    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(((String)key).replace("\uFEFF", "").toLowerCase());
    }
}
