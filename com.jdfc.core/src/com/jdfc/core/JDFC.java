package com.jdfc.core;

import java.util.ResourceBundle;

public final class JDFC {

    /** Qualified build version of the JDFC core library. */
    public static final String VERSION;

    /** Absolute URL of the current JDFC home page */
    public static final String HOMEURL;

    /** Name of the runtime package of this build */
    public static final String RUNTIMEPACKAGE;

    static {
        VERSION = "version";
        HOMEURL = "url";
        RUNTIMEPACKAGE = "package";
    }

    private JDFC() {
    }
}
