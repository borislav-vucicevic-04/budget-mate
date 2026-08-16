package com.borislavvucicevic.budgetmate.enums;

import androidx.annotation.NonNull;

import java.util.Arrays;

/**
 * Defines the custom application-specific mapping for Firebase Authentication error codes.
 *
 * <p>This enum is used to convert raw string error codes returned by the Firebase SDK
 * into strongly-typed values for consistent internal error handling and localization.</p>
 */
public enum FirebaseAuthErrorCodes {
  /**
   * Indicates that the format of the provided email address is malformed or invalid.
   */
  ERROR_INVALID_EMAIL,

  /**
   * Indicates that an account already exists with the given email address.
   */
  ERROR_EMAIL_ALREADY_IN_USE,

  /**
   * Indicates that user provided wrong email or password.
   */
  ERROR_INVALID_CREDENTIAL,

  /**
   * Indicates that the user account has been disabled or suspended by an administrator.
   */
  ERROR_USER_DISABLED,
  /**
   * Indicates that the user's email is not verified.
   * */
  ERROR_EMAIL_NOT_VERIFIED,

  /**
   * Indicates that user sent too many requests
   * */
  ERROR_TOO_MANY_REQUESTS,

  /**
   * Indicates that there is no specific constant for this error code.
   * */
  NOT_SPECIFIED;

  /**
   * Parses a raw string value into its corresponding enum constant.
   *
   * @param value the raw error code string to evaluate.
   * @return the matching {@code FirebaseAuthErrorCodes} constant, or {@code NOT_SPECIFIED} if the
   *         input is {@code null} or does not match any known error code
   */
  @NonNull
  public static FirebaseAuthErrorCodes parse(String value) {
    if (value == null) {
      return NOT_SPECIFIED;
    }

    String normalized = value.trim();
    return Arrays.stream(FirebaseAuthErrorCodes.values())
            .filter(status -> status.name().equalsIgnoreCase(normalized))
            .findFirst()
            .orElse(NOT_SPECIFIED);
  }
}