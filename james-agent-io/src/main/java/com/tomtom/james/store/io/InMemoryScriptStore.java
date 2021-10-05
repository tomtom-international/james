package com.tomtom.james.store.io;

import java.util.HashMap;
import java.util.Map;

public final class InMemoryScriptStore implements ScriptsStore{

    private Map<String,String> scriptsMap = new HashMap();

    @Override
    public String loadScriptByName(final String scriptName) {
        return scriptsMap.get(scriptName);
    }

    public void registerFile(String name, String content){
        scriptsMap.put(name, content);
    }
}
