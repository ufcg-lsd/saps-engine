package org.fogbowcloud.saps.engine.core.dispatcher.email.keystone;

import java.io.IOException;

public class KeystoneException extends Exception {

    public KeystoneException(String s) {
        super(s);
    }

    public KeystoneException(String s, Exception e) {
        super(s, e);
    }
}
