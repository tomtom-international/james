package com.tomtom.james.store;

import com.tomtom.james.store.informationpoints.io.ScriptsStore;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;

public class FileScriptStore implements ScriptsStore {

    private File informationPointDir;

    public FileScriptStore(File informationPointFile){
        this.informationPointDir = informationPointFile.getParentFile();
    }

    @Override
    public String loadScriptByName(final String scriptName) {
        try {
            return Files.lines(informationPointDir.toPath().resolve(scriptName)).collect(Collectors.joining("\n"));
        } catch (IOException e) {
            //TODO log
            return null;
        }
    }
}
