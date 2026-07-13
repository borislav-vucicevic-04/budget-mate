package com.borislavvucicevic.budgetmate.models.exceptions;

/**
 * Signals that an authentication or authorization operation has failed.
 *
 * <p>This exception wraps a specific machine-readable error code alongside
 * the standard error message and cause to allow precise programmatic handling
 * of security failures.</p>
 */
public class AuthException extends RuntimeException {

  /**
   * The machine-readable error code associated with this exception.
   */
  private final String errorCode;

  /**
   * Constructs a new authentication exception with the specified error code and detail message.
   *
   * @param errorCode the specific machine-readable error code
   * @param message   the detail message explaining the reason for the failure
   */
  public AuthException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  /**
   * Constructs a new authentication exception with the specified error code, detail message,
   * and the underlying cause.
   *
   * @param errorCode the specific machine-readable error code
   * @param message   the detail message explaining the reason for the failure
   * @param cause     the underlying cause of the failure, or {@code null} if unknown
   */
  public AuthException(String errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  /**
   * Retrieves the machine-readable error code associated with this exception.
   *
   * @return the error code string
   */
  public String getErrorCode() {
    return errorCode;
  }
}
