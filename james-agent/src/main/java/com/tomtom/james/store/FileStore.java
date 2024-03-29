/*
 * Copyright 2017 TomTom International B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tomtom.james.store;

import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.store.informationpoints.io.ConfigParser;
import com.tomtom.james.store.informationpoints.io.ConfigIOFactory;
import com.tomtom.james.store.informationpoints.io.ConfigWriter;
import com.tomtom.james.store.informationpoints.io.InformationPointDTO;
import com.tomtom.james.store.informationpoints.io.InformationPointStore;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

class FileStore implements InformationPointStore {

    private static final Logger LOG = Logger.getLogger(FileStore.class);

    private final ConfigParser configParser;
    private final ConfigWriter configWriter;
    private final String filePath;

    FileStore(String filePath) {
        this.filePath = filePath;
        configParser = ConfigIOFactory.getInstance().getParser(filePath).orElseThrow(
            () -> new IllegalArgumentException(String.format("Config file: %s has unsuported extension for reading ", filePath)));
        configWriter = ConfigIOFactory.getInstance().getWriter(filePath).orElseThrow(
            () -> new IllegalArgumentException(String.format("Config file: %s has unsuported extension for writing ", filePath)));
    }

    @Override
    public void store(Collection<InformationPoint> informationPoints) {
        try {
            final File file = new File(filePath);
            try(final FileOutputStream outputStream = new FileOutputStream(file)){
                configWriter.storeConfiguration(outputStream, informationPoints);
            }
        } catch (IOException e) {
            LOG.error("Information points write failed", e);
        }
    }

    @Override
    public Collection<InformationPoint> restore() {
        try {
            final File informationPointFile = new File(filePath);
            final FileScriptStore fileScriptStore = new FileScriptStore(informationPointFile);

            try(final FileInputStream fileInputStream = new FileInputStream(informationPointFile)) {
                final Collection<InformationPointDTO> dtos =
                    this.configParser.parseConfiguration(fileInputStream,
                                                         fileScriptStore
                                                        );
                return dtos.stream().map(InformationPointDTO::toInformationPoint)
                           .collect(Collectors.toList());
            }
        } catch (FileNotFoundException e) {
            LOG.warn(() -> "Information point store file not found at " + filePath);
            return Collections.emptyList();
        } catch (IOException e) {
            throw new RuntimeException("Information points read failed", e);
        }
    }

}
