package com.truward.di;

/**
 * Exception, that indicates certain illicit condition when operating in the injection context
 *
 * @author Alexander Shabanov
 */
public class InjectionException extends RuntimeException {
  public InjectionException(String message) {
    super(message);
  }

  public InjectionException(String message, Throwable cause) {
    super(message, cause);
  }

  public InjectionException(Throwable cause) {
    super(cause);
  }
}
