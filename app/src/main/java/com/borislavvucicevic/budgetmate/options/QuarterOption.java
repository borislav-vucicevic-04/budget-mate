package com.borislavvucicevic.budgetmate.options;

import androidx.annotation.NonNull;

import com.borislavvucicevic.budgetmate.enums.Quarter;

/**
 * Represents a selectable quarter option used in the application's
 * user interface.
 *
 * <p>Each option contains a user-visible display name and the corresponding
 * {@link Quarter} enum value used internally by the application.</p>
 *
 * <p>The {@link #toString()} method returns the display name so that instances
 * of this class can be displayed directly in UI components such as spinners.</p>
 *
 * @see Quarter
 */
public class QuarterOption {

  /**
   * User-visible name of the quarter.
   */
  private final String displayName;

  /**
   * Quarter value associated with this option.
   */
  private final Quarter quarter;

  /**
   * Creates a new quarter option.
   *
   * @param displayName the user-visible name of the quarter
   * @param quarter the {@link Quarter} value associated with this option
   */
  public QuarterOption(String displayName, Quarter quarter) {
    this.displayName = displayName;
    this.quarter = quarter;
  }

  /**
   * Returns the user-visible name of the quarter.
   *
   * @return the quarter display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Returns the {@link Quarter} value associated with this option.
   *
   * @return the corresponding quarter
   */
  public Quarter getQuarter() {
    return quarter;
  }

  /**
   * Returns the display name of this quarter option.
   *
   * <p>This allows the object to be displayed directly in UI components
   * such as Android spinners.</p>
   *
   * @return the user-visible quarter name
   */
  @NonNull
  @Override
  public String toString() {
    return this.getDisplayName();
  }
}