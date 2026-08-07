package com.borislavvucicevic.budgetmate.options;

import androidx.annotation.NonNull;

import com.borislavvucicevic.budgetmate.enums.Month;

/**
 * Represents a selectable month option used in the application's
 * user interface.
 *
 * <p>Each option contains a user-visible display name and the corresponding
 * {@link Month} enum value used internally by the application.</p>
 *
 * <p>The {@link #toString()} method returns the display name so that instances
 * of this class can be used directly in UI components such as spinners.</p>
 *
 * @see Month
 */
public class MonthOption {

  /**
   * User-visible name of the month.
   */
  private final String displayName;

  /**
   * Month value associated with this option.
   */
  private final Month month;

  /**
   * Creates a new month option.
   *
   * @param displayName the user-visible name of the month
   * @param quarter the {@link Month} value associated with this option
   */
  public MonthOption(String displayName, Month quarter) {
    this.displayName = displayName;
    this.month = quarter;
  }

  /**
   * Returns the user-visible name of the month.
   *
   * @return the month display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Returns the {@link Month} value associated with this option.
   *
   * @return the corresponding month
   */
  public Month getMonth() {
    return month;
  }

  /**
   * Returns the display name of this month option.
   *
   * <p>This allows the object to be displayed directly in UI components
   * such as Android spinners.</p>
   *
   * @return the user-visible month name
   */
  @NonNull
  @Override
  public String toString() {
    return this.getDisplayName();
  }
}