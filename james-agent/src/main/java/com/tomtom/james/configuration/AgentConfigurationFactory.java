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

package com.tomtom.james.configuration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class AgentConfigurationFactory {

    private static final String ENV_PROPERTY_NAME = "james.configurationPath";

    private AgentConfigurationFactory() {
    }

    public static AgentConfiguration create() throws ConfigurationInitializationException {
        String configurationPath = System.getProperty(ENV_PROPERTY_NAME);
        if (configurationPath == null) {
            throw new ConfigurationInitializationException("Path to agent configuration not set, check " + ENV_PROPERTY_NAME + " property");
        }
        try (FileInputStream fis = new FileInputStream(configurationPath)) {
            YAMLConfiguration yamlConfiguration = new YAMLConfiguration(fis);
            return new AgentConfigurationFacade(yamlConfiguration);
        } catch (FileNotFoundException e) {
            throw new ConfigurationInitializationException("Unable to open configuration file " + configurationPath + ", file not found");
        } catch (IOException e) {
            throw new ConfigurationInitializationException("Unable to open configuration file " + configurationPath, e);
        }
    }

}
