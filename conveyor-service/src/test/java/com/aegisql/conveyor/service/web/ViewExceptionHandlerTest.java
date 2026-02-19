package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.api.PlacementResult;
import org.apache.tomcat.util.http.fileupload.impl.FileCountLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.ModelAndView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewExceptionHandlerTest {

    private final ViewExceptionHandler handler = new ViewExceptionHandler();

    @Test
    void dashboardMultipartExceptionRedirectsToDashboardWithFlashError() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/dashboard/test/place");
        request.addHeader("Accept", "text/html");
        request.addHeader("Referer", "http://localhost/dashboard?name=collector&tab=tab-tester");
        FlashMap flashMap = new FlashMap();
        request.setAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE, flashMap);

        Object result = handler.handleMultipartException(new MultipartException("upload failed"), request);

        ModelAndView modelAndView = assertInstanceOf(ModelAndView.class, result);
        assertEquals("redirect:/dashboard?name=collector&tab=tab-tester", modelAndView.getViewName());
        assertTrue(flashMap.containsKey("error"));
        assertTrue(String.valueOf(flashMap.get("error")).contains("Multipart request failed"));
    }

    @Test
    void apiMultipartExceptionReturnsBadRequestEnvelope() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/part/collector/1/USER");
        request.addHeader("Accept", "application/json");

        Object result = handler.handleMultipartException(new FileCountLimitExceededException("attachment", 1L), request);

        ResponseEntity<?> response = assertInstanceOf(ResponseEntity.class, result);
        assertEquals(400, response.getStatusCode().value());
        PlacementResult<?> body = assertInstanceOf(PlacementResult.class, response.getBody());
        assertEquals("BAD_REQUEST", body.getErrorCode());
        assertTrue(body.getErrorMessage().contains("too many multipart fields/files"));
    }
}
