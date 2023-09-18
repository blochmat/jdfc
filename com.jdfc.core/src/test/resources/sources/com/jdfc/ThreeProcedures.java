package com.jdfc.interprocedural;

public class ThreeProcedures {

    public void defineA() {
        int a = 42;
        defineB(a);
    }

    public void defineB(int a) {
        int b = a + 1;
        useBoth(a, b);
    }

    public void useBoth(int a, int b) {
        System.out.println(a + b);
    }
}
