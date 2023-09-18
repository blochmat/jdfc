package com.jdfc.interprocedural;

public class TwoProcedures {

    public void defineVariable() {
        int data = 42; // Definition
        useVariable(data);
    }

    public void useVariable(int data) {
        System.out.println("Data: " + data);  // Use
    }
}
