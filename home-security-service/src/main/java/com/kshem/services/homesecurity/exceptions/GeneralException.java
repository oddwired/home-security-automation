package com.kshem.services.homesecurity.exceptions;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class GeneralException extends RuntimeException {
  protected HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

  public GeneralException(String message) {
    super(message);
  }

  public GeneralException(String message, Throwable cause) {
    super(message, cause);
  }

  public GeneralException(String message, HttpStatus status) {
    super(message);
    this.status = status;
  }

  public GeneralException(String message, Throwable cause, HttpStatus status) {
    super(message, cause);
    this.status = status;
  }
}
