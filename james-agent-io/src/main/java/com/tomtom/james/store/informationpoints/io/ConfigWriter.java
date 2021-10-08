package com.tomtom.james.store.informationpoints.io;

import com.tomtom.james.common.api.informationpoint.InformationPoint;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

public interface ConfigWriter extends BaseConfigIO{

    void storeConfiguration(final OutputStream outputStream, Collection<InformationPoint> informationPoints) throws IOException;
}
