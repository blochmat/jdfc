package com.jdfc.agent.rt;

import org.jacoco.core.runtime.AbstractRuntime;
import org.jacoco.core.runtime.AgentOptions;
import org.jacoco.core.runtime.RuntimeData;

import java.net.InetAddress;
import java.util.logging.Logger;

public class Agent {

    private static Agent singleton;
    private final Logger logger = Logger.getLogger("Logger");
    private final AgentOptions options;
    private final RuntimeData data;


    public static synchronized Agent getInstance(final AgentOptions options)
            throws Exception {
        if (singleton == null) {
            final Agent agent = new Agent(options);
            agent.startup();
            java.lang.Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    agent.shutdown();
                }
            });
            singleton = agent;
        }
        return singleton;
    }

    Agent(final AgentOptions options) {
        this.options = options;
        this.data = new RuntimeData();
    }

    public RuntimeData getData() {
        return data;
    }

    public void startup() throws Exception {
        try {
            String sessionId = options.getSessionId();
            if (sessionId == null) {
                sessionId = createSessionId();
            }
            data.setSessionId(sessionId);
            //output = createAgentOutput();
        } catch (final Exception e) {
            throw e;
        }
    }

    private String createSessionId() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (final Exception e) {
            // Also catch platform specific exceptions (like on Android) to
            // avoid bailing out here
            host = "unknownhost";
        }
        return host + "-" + AbstractRuntime.createRandomId();
    }

    public void shutdown() {
        // Write Output
    }
}
