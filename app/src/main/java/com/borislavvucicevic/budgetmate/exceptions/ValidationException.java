package com.borislavvucicevic.budgetmate.exceptions;

/**
 * Custom runtime exception thrown during form or input validation failures.
 * <p>
 * This exception encapsulates an error message along with an optional Android UI
 * component resource ID (such as {@code R.id.editText}). The target view ID allows
 * UI controllers to programmatically locate and display the error message directly
 * on the specific component that failed validation.
 * </p>
 *
 * @see java.lang.RuntimeException
 */
public class ValidationException extends RuntimeException {
  private final Integer fieldID;
  public ValidationException(String message, Integer viewID) {
    super(message);
    this.fieldID = viewID;
  }
  public Integer getViewID() {
    return fieldID;
  }
}
