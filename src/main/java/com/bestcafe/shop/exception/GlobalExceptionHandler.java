package com.bestcafe.shop.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.time.LocalDateTime;

import javax.management.modelmbean.XMLParseException;

import com.fasterxml.jackson.core.JsonParseException;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final String BAD_REQUEST = "Bad Request";
    private static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
    private static final String NOT_FOUND = "Resource not found";

    @ExceptionHandler({JsonParseException.class, XMLParseException.class})
    public ResponseEntity<ErrorResponse> handleJsonException(Exception ex, WebRequest request) {
        return getErrorResponseResponseEntity(HttpStatus.BAD_REQUEST, BAD_REQUEST, ex, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(Exception ex, WebRequest request) {
        return getErrorResponseResponseEntity(HttpStatus.NOT_FOUND, NOT_FOUND, ex, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        return getErrorResponseResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR, ex, request);
    }

    private ResponseEntity<ErrorResponse> getErrorResponseResponseEntity(
            final HttpStatus badRequest,
            final String Bad_Request,
            final Exception ex,
            final WebRequest request) {
        var errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                badRequest.value(),
                Bad_Request,
                ex.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(errorResponse, badRequest);
    }


}
