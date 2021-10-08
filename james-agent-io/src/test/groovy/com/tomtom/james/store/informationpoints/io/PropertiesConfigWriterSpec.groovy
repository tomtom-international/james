package com.tomtom.james.store.informationpoints.io

import spock.lang.Specification
import java.util.stream.Collectors

class PropertiesConfigWriterSpec extends Specification{

    def samplePropertiesString = "class!method={\"sampleRate\":100,\"successSampleRate\":100.0,\"errorSampleRate\":100.0,\"successExecutionThreshold\":-1,\"metadata\":{},\"script\":[\"script\"]}"

    def "Should read and store information point in properties format"() {
        def configIO = new PropertiesConfigIO();
        when:
        def dtos = configIO.parseConfiguration(new ByteArrayInputStream(samplePropertiesString.getBytes()), new NoopScriptsStore());
        def informationPoints = dtos.stream().map(InformationPointJsonDTO::toInformationPoint).collect(Collectors.toList());
        def output = new ByteArrayOutputStream()
        configIO.storeConfiguration(output,informationPoints);
        def outputString = new String(output.toByteArray());

        then:
        informationPoints.size() == 1
        informationPoints.get(0).className == "class"
        informationPoints.get(0).methodName == "method"
        informationPoints.get(0).script.ifPresent(script->script == ["script"] )
        outputString.contains("class")
        outputString.contains("method")
        outputString.contains("script")
    }
}
