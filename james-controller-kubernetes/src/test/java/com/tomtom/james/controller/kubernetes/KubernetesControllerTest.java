package com.tomtom.james.controller.kubernetes;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.tomtom.james.common.api.configuration.JamesControllerConfiguration;
import com.tomtom.james.common.api.configuration.StructuredConfiguration;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.informationpoint.InformationPointService;
import com.tomtom.james.common.log.Logger;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapBuilder;
import io.kubernetes.client.openapi.models.V1ObjectMetaBuilder;
import io.kubernetes.client.util.Watch;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesControllerTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Mock
    private InformationPointService informationPointService;
    @Mock
    private JamesControllerConfiguration jamesControllerConfiguration;
    private final List<InformationPoint> informationPoints = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KubernetesController startKubernetesController() {
        Logger.setCurrentLogLevel(Logger.Level.DEBUG);
        final StructuredConfiguration configuration = mock(StructuredConfiguration.class);
        final StructuredConfiguration nameSpaceConfiguration = mock(StructuredConfiguration.class);
        final StructuredConfiguration urlConfiguration = mock(StructuredConfiguration.class);
        final StructuredConfiguration labelsConfiguration = mock(StructuredConfiguration.class);
        final StructuredConfiguration appConfiguration = mock(StructuredConfiguration.class);
        when(jamesControllerConfiguration.getProperties())
            .thenReturn(Optional.of(configuration));
        when(configuration.get("url")).thenReturn(Optional.of(urlConfiguration));
        when(urlConfiguration.asString()).thenReturn(wireMockRule.baseUrl());
        when(configuration.get("namespace")).thenReturn(Optional.of(nameSpaceConfiguration));
        when(nameSpaceConfiguration.asString()).thenReturn("dev");
        when(configuration.get("labels")).thenReturn(Optional.of(labelsConfiguration));
        when(labelsConfiguration.asMap()).thenReturn(Collections.singletonMap("app", appConfiguration));
        when(appConfiguration.asString()).thenReturn("my-app");

        final KubernetesController kubernetesController = new KubernetesController();
        kubernetesController.initialize(
            jamesControllerConfiguration,
            informationPointService,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
        return kubernetesController;
    }

    @Test
    public void shouldRegister() throws InterruptedException, IOException {
        final V1ConfigMap configMap = new V1ConfigMapBuilder()
            .withApiVersion("v1")
            .withKind("ConfigMap")
            .withMetadata(new V1ObjectMetaBuilder().withName("james-test").addToLabels("app", "qa-webservice").build())
            .addToData("app.properties", "Class!method={ \"version\":\"1\", \"script\":[\"boom\"]}")
            .build();
        final Watch.Response<V1ConfigMap> response = new Watch.Response<>("ADDED", configMap);

        stubFor(get(urlEqualTo("/api/v1/namespaces/dev/configmaps?labelSelector=app%3Dmy-app&watch=true"))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody(objectMapper.writeValueAsString(response))));

        final KubernetesController controller = startKubernetesController();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        doAnswer(invocationOnMock -> {
            informationPoints.add(invocationOnMock.getArgument(0, InformationPoint.class));
            countDownLatch.countDown();
            return null;
        }).when(informationPointService).addInformationPoint(any(InformationPoint.class));
        countDownLatch.await(3, TimeUnit.SECONDS);
        controller.close();
        assertThat(informationPoints)
            .hasSize(1)
            .allSatisfy(ip -> {
                assertThat(ip.getClassName()).isEqualTo("Class");
                assertThat(ip.getMethodName()).isEqualTo("method");
                assertThat(ip.getScript()).hasValue("boom");
            });
    }
}
