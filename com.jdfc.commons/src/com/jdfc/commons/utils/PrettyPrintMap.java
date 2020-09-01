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
            sb.append('=').append("\n");
            Object foo = entry.getValue();
            if (foo != null){
                sb.append("Sure, something is there of type: ").append(foo.getClass().getName());
            } else {
                sb.append("Not that good news, boy.");
            }
            sb.append('"');
            if (iter.hasNext()) {
                sb.append(',').append('\n');
            }
        }
        return sb.toString();
    }
}
