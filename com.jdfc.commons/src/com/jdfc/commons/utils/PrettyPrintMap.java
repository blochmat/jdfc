package com.jdfc.commons.utils;

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
            sb.append(" = ");
            Object foo = entry.getValue();
            if (foo instanceof List){
                sb.append("\n");
                sb.append(Arrays.toString(((List<?>) foo).toArray()));
            } else if (foo instanceof Set){
                sb.append("\n");
                sb.append(Arrays.toString(((Set<?>) foo).toArray()));
            } else {
                sb.append(foo.toString());
            }
            if (iter.hasNext()) {
                sb.append(',').append('\n');
            }
        }
        return sb.toString();
    }
}
