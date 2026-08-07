package com.borislavvucicevic.budgetmate.exceptions;

/**
 * Runtime exception used to indicate errors related to database operations
 * within the BudgetMate application.
 *
 * <p>This exception can be thrown when an operation involving persistent
 * application data cannot be completed successfully, such as retrieving,
 * creating, updating, or deleting records from the database.</p>
 *
 * <p>Because this class extends {@link RuntimeException}, callers are not
 * required to explicitly declare or catch the exception.</p>
 *
 * @see RuntimeException
 */
public class DatabaseException extends RuntimeException {

  /**
   * Creates a new database exception with the specified error message.
   *
   * @param message description of the database-related error
   */
  public DatabaseException(String message) {
    super(message);
  }

  /**
   * Creates a new database exception with the specified error message and
   * underlying cause.
   *
   * <p>This constructor is useful when another exception causes a database
   * operation to fail and the original cause should be preserved for
   * debugging, logging, or error handling purposes.</p>
   *
   * @param message description of the database-related error
   * @param cause the underlying exception that caused the database operation
   *              to fail
   */
  public DatabaseException(String message, Throwable cause) {
    super(message, cause);
  }
}