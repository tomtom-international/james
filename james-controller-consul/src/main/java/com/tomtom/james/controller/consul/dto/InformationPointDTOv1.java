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

package com.tomtom.james.controller.consul.dto;

import com.tomtom.james.common.api.informationpoint.Metadata;

import java.util.List;

public class InformationPointDTOv1 extends Versioned {

    private List<String> script;
    private Integer sampleRate;
    private Metadata metadata;

    public List<String> getScript() {
        return script;
    }

    public Integer getSampleRate() {
        return sampleRate;
    }

    public Metadata getMetadata() {
        return metadata;
    }
}
