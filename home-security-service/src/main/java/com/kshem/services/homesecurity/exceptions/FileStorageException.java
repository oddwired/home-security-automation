package com.kshem.services.homesecurity.exceptions;

import org.springframework.http.HttpStatus;

public class FileStorageException extends GeneralException {

  public FileStorageException(String message) {
    super(message, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  public FileStorageException(String message, Throwable cause) {
    super(message, cause);
  }

  public FileStorageException(String message, HttpStatus status) {
    super(message, status);
  }

  public FileStorageException(String message, Throwable cause, HttpStatus status) {
    super(message, cause, status);
  }
}
