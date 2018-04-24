package com.tomtom.james.controller.webservice.handlers.v1;

import com.sun.net.httpserver.HttpExchange;
import com.tomtom.james.common.api.ClassScanner;
import com.tomtom.james.controller.webservice.HTTPContentType;
import com.tomtom.james.controller.webservice.HTTPStatus;
import com.tomtom.james.controller.webservice.ResponseBuilder;
import com.tomtom.james.controller.webservice.handlers.AbstractHttpHandler;

import java.io.IOException;
import java.util.List;

public class ClassScannerHandler extends AbstractHttpHandler {
    private static final String PROCESSED_CLASSES = "processedclasses";
    private static final String CLASS_STRUCTURE = "classstructure";
    private static final String IGNORED_PACKAGES = "ignoredpackages";
    private ClassScanner classScanner;

    public ClassScannerHandler(ClassScanner classScanner) {
        this.classScanner = classScanner;
    }

    @Override
    protected void doHandle(HttpExchange httpExchange, List<String> pathParams) throws IOException {
        if (isGet.test(httpExchange) && pathParams.isEmpty()) {
            ResponseBuilder.forExchange(httpExchange)
                    .withResponseBody(asJSONBytes(classScanner.getStatistics()), HTTPContentType.APPLICATION_JSON)
                    .sendResponse(HTTPStatus.OK);

        } else if (isGet.test(httpExchange) && pathParams.size() == 1 && pathParams.get(0).equals(PROCESSED_CLASSES)) {
            ResponseBuilder.forExchange(httpExchange)
                    .withResponseBody(asJSONBytes(classScanner.getProcessedClassesInfos()), HTTPContentType.APPLICATION_JSON)
                    .sendResponse(HTTPStatus.OK);

        } else if (isGet.test(httpExchange) && pathParams.size() == 1 && pathParams.get(0).equals(CLASS_STRUCTURE)) {
            ResponseBuilder.forExchange(httpExchange)
                    .withResponseBody(asJSONBytes(classScanner.getClassStructureInfos()), HTTPContentType.APPLICATION_JSON)
                    .sendResponse(HTTPStatus.OK);
        } else if (isGet.test(httpExchange) && pathParams.size() == 1 && pathParams.get(0).equals(IGNORED_PACKAGES)) {
            ResponseBuilder.forExchange(httpExchange)
                    .withResponseBody(asJSONBytes(classScanner.getIgnoredPackages()), HTTPContentType.APPLICATION_JSON)
                    .sendResponse(HTTPStatus.OK);
        } else {
            ResponseBuilder.forExchange(httpExchange)
                    .sendResponse(HTTPStatus.METHOD_NOT_ALLOWED);
        }
    }

}
