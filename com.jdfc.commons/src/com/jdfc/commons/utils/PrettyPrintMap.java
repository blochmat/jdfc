package com.jdfc.commons.utils;

import com.jdfc.commons.data.Pair;

import java.util.*;

public class PrettyPrintMap<K, V> {
    private Map<K, V> map;

    public PrettyPrintMap(Map<K, V> map) {
        this.map = map;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<K, V>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<K, V> entry = iter.next();
            sb.append(entry.getKey());
            sb.append('=').append("\n");
            Object foo = entry.getValue();
            if (foo instanceof List){
                sb.append(Arrays.toString(((List<?>) foo).toArray()));
            } else if (foo instanceof Set){
                sb.append(Arrays.toString(((Set<?>) foo).toArray()));
            } else if (foo instanceof Pair){
                sb.append(foo.toString());
            } else if (foo instanceof Boolean) {
                sb.append(foo.toString());
            }
            sb.append('"');
            if (iter.hasNext()) {
                sb.append(',').append('\n');
            }
        }
        return sb.toString();
    }
}
