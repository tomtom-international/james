package com.tomtom.james.store.io;

public class NoopScriptsStore implements ScriptsStore {

    @Override
    public String loadScriptByName(final String scriptName) {
        throw new IllegalArgumentException(
            "This configuration shouldn't refer to any file. Found reference to file: " + scriptName + "!");
    }
}
