package com.kshem.services.homesecurity.exceptions;

import com.kshem.services.homesecurity.models.ErrorResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@Order(2)
public class UnhandledExceptionHandler {
  @ExceptionHandler(Exception.class)
  @ResponseBody
  public ResponseEntity<ErrorResponse> handleException(Exception e) {
    ErrorResponse errorInfo = new ErrorResponse(500, e.getMessage());
    return new ResponseEntity<>(errorInfo, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
