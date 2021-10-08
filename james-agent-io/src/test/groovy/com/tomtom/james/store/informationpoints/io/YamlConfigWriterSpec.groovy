package com.tomtom.james.store.informationpoints.io

import spock.lang.Specification
import java.util.stream.Collectors

class YamlConfigWriterSpec extends Specification {

    def sampleYamlString = "---\n" +
            "class!method:\n" +
            "  sampleRate: 100\n" +
            "  successSampleRate: 100.0\n" +
            "  errorSampleRate: 100.0\n" +
            "  successExecutionThreshold: -1\n" +
            "  metadata: {}\n" +
            "  baseScript: null\n" +
            "  script: \"script\"\n"

    def "Should read and store information point in properties format"() {
        def configIO = new YamlConfigIO();
        when:
        def dtos = configIO.parseConfiguration(new ByteArrayInputStream(sampleYamlString.getBytes()), new NoopScriptsStore());
        def informationPoints = dtos.stream().map(InformationPointYamlDTO::toInformationPoint).collect(Collectors.toList());
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
