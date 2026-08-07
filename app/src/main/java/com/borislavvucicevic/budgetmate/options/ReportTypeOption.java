package com.borislavvucicevic.budgetmate.options;

import androidx.annotation.NonNull;

import com.borislavvucicevic.budgetmate.enums.ReportType;

/**
 * Represents a selectable report type option used in the application's
 * user interface.
 *
 * <p>Each option contains a user-visible display name and the corresponding
 * {@link ReportType} value used internally by the application.</p>
 *
 * <p>The {@link #toString()} method returns the display name so that instances
 * of this class can be displayed directly in UI components such as spinners.</p>
 *
 * @see ReportType
 */
public class ReportTypeOption {

  /**
   * User-visible name of the report type.
   */
  private final String displayName;

  /**
   * Report type associated with this option.
   */
  private final ReportType type;

  /**
   * Creates a new report type option.
   *
   * @param displayName the user-visible name of the report type
   * @param type the {@link ReportType} value associated with this option
   */
  public ReportTypeOption(String displayName, ReportType type) {
    this.displayName = displayName;
    this.type = type;
  }

  /**
   * Returns the user-visible name of the report type.
   *
   * @return the report type display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Returns the {@link ReportType} value associated with this option.
   *
   * @return the corresponding report type
   */
  public ReportType getType() {
    return type;
  }

  /**
   * Returns the display name of this report type option.
   *
   * <p>This allows the object to be displayed directly in UI components
   * such as Android spinners.</p>
   *
   * @return the user-visible report type name
   */
  @NonNull
  @Override
  public String toString() { return  this.getDisplayName(); }
}