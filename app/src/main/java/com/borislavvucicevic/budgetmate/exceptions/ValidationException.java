package com.borislavvucicevic.budgetmate.exceptions;

import androidx.annotation.Nullable;

/**
 * Runtime exception used to represent validation failures in user input
 * or application forms.
 *
 * <p>This exception stores a validation error message together with an
 * optional Android view resource identifier. The view identifier can be used
 * by an activity or other UI controller to determine which input field caused
 * the validation error.</p>
 *
 * <p>If the validation error is not associated with a specific input field,
 * the stored view identifier may be {@code null}.</p>
 *
 * <p>Because this class extends {@link RuntimeException}, callers are not
 * required to explicitly declare or catch the exception.</p>
 *
 * @see RuntimeException
 */
public class ValidationException extends RuntimeException {

  /**
   * Resource identifier of the UI field associated with the validation
   * error.
   *
   * <p>The value may be {@code null} when the validation failure does not
   * correspond to a specific view.</p>
   */
  @Nullable
  private final Integer fieldID;

  /**
   * Creates a new validation exception with the specified error message and
   * associated view identifier.
   *
   * @param message validation error message describing why the input is invalid
   * @param viewID resource identifier of the UI field that failed validation,
   *               or {@code null} if the error is not associated with a
   *               specific field
   */
  public ValidationException(String message, @Nullable Integer viewID) {
    super(message);
    this.fieldID = viewID;
  }

  /**
   * Returns the resource identifier of the UI field associated with the
   * validation error.
   *
   * @return resource identifier of the invalid input field, or {@code null}
   *         if no specific field is associated with the validation failure
   */
  @Nullable
  public Integer getViewID() {
    return fieldID;
  }
}