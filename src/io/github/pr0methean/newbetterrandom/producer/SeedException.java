package io.github.pr0methean.newbetterrandom.producer;

import java.io.IOException;

public class SeedException extends RuntimeException {
  public SeedException(String message, Throwable cause) {
    super(message, cause);
  }

  public SeedException(String message) {
    super(message);
  }
}
