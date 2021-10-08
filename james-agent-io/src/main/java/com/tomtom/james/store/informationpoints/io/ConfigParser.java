package com.tomtom.james.store.informationpoints.io;

import com.tomtom.james.common.api.informationpoint.InformationPoint;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

public interface ConfigParser extends BaseConfigIO{

    Collection<InformationPointDTO> parseConfiguration(final InputStream inStream, ScriptsStore fileScriptStore)
        throws IOException;
}
