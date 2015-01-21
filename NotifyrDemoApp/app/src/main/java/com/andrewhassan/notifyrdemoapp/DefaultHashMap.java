package com.andrewhassan.notifyrdemoapp;

import java.util.HashMap;

/**
 * Created by Applepie on 1/15/2015.
 */
public class DefaultHashMap<K, V> extends HashMap<K, V> {
    public V getDefault(K key, V defaultValue) {
        if (containsKey(key)) {
            return get(key);
        }

        return defaultValue;
    }
}
