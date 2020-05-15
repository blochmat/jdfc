package main.java.com.jdfc.core;

import java.util.ResourceBundle;

/**
 * Static meta information about JDFC core library.
 */
public final class JDFC {
    /** Qualified build version of the JDFC core library. */
    public static final String VERSION;

    /** Absolute URL of the current JDFC home page */
    public static final String HOMEURL;

    /** Name of the runtime package of this build */
    public static final String RUNTIMEPACKAGE;

    static {
        final ResourceBundle bundle = ResourceBundle
                .getBundle("org.jacoco.core.jacoco");
        VERSION = bundle.getString("VERSION");
        HOMEURL = bundle.getString("HOMEURL");
        RUNTIMEPACKAGE = bundle.getString("RUNTIMEPACKAGE");
    }

    private JDFC() {
    }
}
