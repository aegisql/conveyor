package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.api.PlacementStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.tomcat.util.http.fileupload.impl.FileCountLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.net.URI;
import java.net.URISyntaxException;

@ControllerAdvice
public class ViewExceptionHandler {

    @ExceptionHandler({MultipartException.class, FileCountLimitExceededException.class})
    public Object handleMultipartException(Exception ex, HttpServletRequest request) {
        String message = multipartErrorMessage(ex);
        if (shouldReturnJson(request)) {
            PlacementResult<Void> body = PlacementResult.<Void>builder()
                    .status(PlacementStatus.REJECTED)
                    .errorCode("BAD_REQUEST")
                    .errorMessage(message)
                    .exceptionClass(ex.getClass().getName())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }
        FlashMap flashMap = request == null ? null : RequestContextUtils.getOutputFlashMap(request);
        if (flashMap != null) {
            flashMap.put("error", message);
        }
        return new ModelAndView("redirect:" + dashboardRedirectTarget(request));
    }

    private boolean shouldReturnJson(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String uri = request.getRequestURI();
        if (StringUtils.hasText(uri) && uri.startsWith("/dashboard")) {
            return false;
        }
        String accept = request.getHeader("Accept");
        if (StringUtils.hasText(accept)) {
            boolean acceptsJson = accept.contains("application/json");
            boolean acceptsHtml = accept.contains("text/html");
            if (acceptsJson) {
                return true;
            }
            if (acceptsHtml) {
                return false;
            }
        }
        return true;
    }

    private String dashboardRedirectTarget(HttpServletRequest request) {
        if (request == null) {
            return "/dashboard";
        }
        String referer = request.getHeader("Referer");
        if (!StringUtils.hasText(referer)) {
            return "/dashboard";
        }
        try {
            URI uri = new URI(referer);
            String path = uri.getPath();
            if (!StringUtils.hasText(path) || !path.startsWith("/dashboard")) {
                return "/dashboard";
            }
            if (StringUtils.hasText(uri.getRawQuery())) {
                return path + "?" + uri.getRawQuery();
            }
            return path;
        } catch (URISyntaxException e) {
            return "/dashboard";
        }
    }

    private String multipartErrorMessage(Throwable throwable) {
        Throwable root = rootCause(throwable);
        String className = root.getClass().getSimpleName();
        String message = root.getMessage();
        if ("FileCountLimitExceededException".equals(className)) {
            return "Request has too many multipart fields/files for server limits. Reduce attachments or form fields.";
        }
        if (StringUtils.hasText(message)) {
            return "Multipart request failed: " + message;
        }
        return "Multipart request failed. Check upload size/count limits.";
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable cursor = throwable;
        int depth = 0;
        while (cursor.getCause() != null && cursor.getCause() != cursor && depth < 10) {
            cursor = cursor.getCause();
            depth++;
        }
        return cursor;
    }
}
