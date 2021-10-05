package com.tomtom.james.store.io;

import com.tomtom.james.common.api.informationpoint.InformationPoint;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

public interface ConfigParserWriter {

    boolean supportsConfigFile(final String name);

    Collection<InformationPointDTO> parseConfiguration(final InputStream inStream, ScriptsStore fileScriptStore)
        throws IOException;

    void storeConfiguration(final OutputStream outputStream, Collection<InformationPoint> informationPoints) throws IOException;
}
