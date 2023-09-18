package com.jdfc.interprocedural;

import java.util.Random;

public class Branch {

    public void defineA() {
        Random random = new Random();
        int a = random.nextInt(1000);
        defineB(a);
    }

    public void defineB(int a) {
        int b = a + 1;
        if(a > 499) {
            diff(a, b);
        } else {
            sum(a, b);
        }
    }

    public void sum(int a, int b) {
        System.out.println(a + b);
    }

    public void diff(int a, int b) {
        System.out.println(a - b);
    }
}
