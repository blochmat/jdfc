package com.jdfc.agent.rt;

import java.io.IOException;

/**
 * Runtime API and MBean agent interface
 */
public interface IAgent {

    /**
     * Returns version of JDFC.
     *
     * @return version of JDFC
     */
    String getVersion();

    /**
     * Returns a current session identifier.
     *
     * @return current session identifier
     */
    String getSessionId();

    /**
     * Sets a session identifier.
     *
     * @param id new session identifier
     */
    void setSessionId(String id);

    /**
     * Reset all coverage information.
     */
    void reset();

    /**
     * Returns current execution data.
     *
     * @param reset if <code>true</code> the current execution data is cleared afterwards
     * @return dump of current execution data in JDFC binary format
     */
    byte[] getExecutionData(boolean reset);

    /**
     * Triggers a dump of the current execution data through the configured output.
     *
     * @param reset
     *              if <code>true</code> the current execution data is cleared afterwards
     * @throws IOException
     *              if the output can't write execution data
     */
    void dump(boolean reset) throws IOException;
}
