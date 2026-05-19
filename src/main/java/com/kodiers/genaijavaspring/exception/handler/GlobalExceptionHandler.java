package com.kodiers.genaijavaspring.exception.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ResponseBody
    @ExceptionHandler(Exception.class)
    public ErrorDTO handleException(Exception ex) {
        log.error("Exception occurred: {}", ex.getMessage());
        return new ErrorDTO(HttpStatus.BAD_REQUEST.getReasonPhrase(), ex.getMessage());
    }
}
