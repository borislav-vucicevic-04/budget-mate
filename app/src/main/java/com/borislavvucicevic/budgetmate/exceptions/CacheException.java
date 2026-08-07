package com.borislavvucicevic.budgetmate.exceptions;

/**
 * Runtime exception used to indicate errors related to the application's
 * caching operations.
 *
 * <p>This exception can be thrown when an operation involving cached data
 * cannot be completed successfully, such as storing, retrieving, converting,
 * or removing cached values.</p>
 *
 * <p>Because this class extends {@link RuntimeException}, callers are not
 * required to explicitly declare or catch the exception.</p>
 *
 * @see RuntimeException
 */
public class CacheException extends RuntimeException {

  /**
   * Creates a new cache exception with the specified error message.
   *
   * @param message description of the cache-related error
   */
  public CacheException(String message) {
    super(message);
  }

  /**
   * Creates a new cache exception with the specified error message and
   * underlying cause.
   *
   * <p>This constructor is useful when a lower-level exception causes the
   * cache operation to fail and should be preserved for debugging or
   * logging purposes.</p>
   *
   * @param message description of the cache-related error
   * @param cause the underlying exception that caused this error
   */
  public CacheException(String message, Throwable cause) {
    super(message, cause);
  }
}
