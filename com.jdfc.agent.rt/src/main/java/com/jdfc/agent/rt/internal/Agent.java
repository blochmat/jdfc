package com.jdfc.agent.rt.internal;

import com.jdfc.agent.rt.IAgent;
import main.java.com.jdfc.core.JDFC;

import java.io.IOException;


/**
 * The agent manages the life cycle of JDFC runtime.
 */
public class Agent implements IAgent {

    private static Agent singleton;

    @Override
    public String getVersion() {
        return JDFC.VERSION;
    }

    @Override
    public String getSessionId() {
        return null;
    }

    @Override
    public void setSessionId(String id) {

    }

    @Override
    public void reset() {

    }

    @Override
    public byte[] getExecutionData(boolean reset) {
        return new byte[0];
    }

    @Override
    public void dump(boolean reset) throws IOException {

    }
}
