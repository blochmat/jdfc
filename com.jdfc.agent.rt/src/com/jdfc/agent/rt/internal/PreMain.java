package com.jdfc.agent.rt.internal;

import com.jdfc.agent.rt.Agent;

import java.lang.instrument.Instrumentation;

public final class PreMain {

    private PreMain(){

    }

    public static void premain(final String options, final Instrumentation inst)
            throws Exception {
        inst.addTransformer(new ClassTransformer());
    }
}
