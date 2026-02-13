package com.kshem.services.homesecurity.exceptions;

import com.kshem.services.homesecurity.models.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice()
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GeneralExceptionHandler {

  @ExceptionHandler(GeneralException.class)
  @ResponseBody
  public ResponseEntity<ErrorResponse> handleGeneralException(GeneralException e) {
    ErrorResponse errorInfo = new ErrorResponse(e.getStatus().value(), e.getMessage());
    return new ResponseEntity<>(errorInfo, e.getStatus());
  }
}
