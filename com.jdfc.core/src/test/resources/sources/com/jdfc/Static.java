package com.jdfc.interprocedural;

public class Static {

    public static void defineAStatic() {
        int a = 42;
        useAStatic(a);
    }

    public void defineA() {
        int a = 42;
        useAStatic(a);
    }

    public static void useAStatic(int a) {
        System.out.println(a);
    }
}
