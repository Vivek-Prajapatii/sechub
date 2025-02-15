// SPDX-License-Identifier: MIT
package com.mercedesbenz.sechub.server.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.ResponseEntity;

class ServerErrorControllerTest {

    private static final String TEST_ERROR_MESSAGE = "my SecHub error message";

    private HttpServletResponse response;
    private HttpServletRequest request;
    private ServerErrorController controllerToTest;
    private DefaultErrorAttributes errorAttributes;

    @BeforeEach
    void beforeEach() {
        response = mock(HttpServletResponse.class);
        request = mock(HttpServletRequest.class);
        errorAttributes = new DefaultErrorAttributes();

        controllerToTest = new ServerErrorController();
        controllerToTest.errorAttributes = errorAttributes;
    }

    @Test
    void using_default_errorattributes_when_response_is_401_the_returns_json_body_contains_the_error_message() {
        /* prepare */
        prepareError(401, TEST_ERROR_MESSAGE);

        /* execute */
        ResponseEntity<ServerError> r = controllerToTest.error(request, response);

        /* test */
        ServerError serverError = r.getBody();
        assertEquals(TEST_ERROR_MESSAGE, serverError.message);

    }

    @Test
    void using_default_errorattributes_when_response_is_501_the_returns_json_body_contains_NOT_the_error_message() {
        /* prepare */
        prepareError(501, TEST_ERROR_MESSAGE);

        /* execute */
        ResponseEntity<ServerError> r = controllerToTest.error(request, response);

        /* test */
        ServerError serverError = r.getBody();
        assertEquals(null, serverError.message);

    }

    void prepareError(int httpStatus, String message) {
        when(response.getStatus()).thenReturn(httpStatus);
        when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(message);
    }

}
