package com.borislavvucicevic.budgetmate.models;

import androidx.annotation.NonNull;

/**
 * Data model representing a selectable currency configuration option within the application UI.
 * <p>
 * This class pairs a user-friendly presentation name with its corresponding structural
 * {@link CurrencyCode}. It is designed primarily to populate UI adapters, such as spinners,
 * where localized text is displayed alongside raw currency enum values.
 * </p>
 */
public class CurrencyOption {
  /**
   * The localized text or descriptive name of the currency displayed to the end user.
   */
  private final String displayName;

  /**
   * The machine-readable ISO standard code matching this option, or {@code null}
   * if this option represents a placeholder or generic home default selection.
   */
  private final CurrencyCode code;

  /**
   * Constructs a new CurrencyOption instance with a display label and backing code.
   *
   * @param displayName The descriptive name or resource string mapping to the item.
   * @param code        The associated currency enumeration value, or {@code null} for default.
   */
  public CurrencyOption(String displayName, CurrencyCode code) {
    this.displayName = displayName;
    this.code = code;
  }

  /**
   * Retrieves the presentation text meant for user interface display.
   *
   * @return A {@link String} containing the visible option name.
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Retrieves the raw backing currency configuration enum.
   *
   * @return The {@link CurrencyCode} representing the financial asset, or {@code null}.
   */
  public CurrencyCode getCode() {
    return code;
  }

  /**
   * Returns a string representation of the object, utilized directly by layout adapters
   * to render content.
   * <p>
   * To prevent formatting crashes when handling default placeholder elements, this override
   * evaluates the internal state: if the backing code object is present, its textual representation
   * is yielded; otherwise, an empty fallback sequence is provided.
   * </p>
   *
   * @return A non-null {@link String} value identifying the currency option tracking data.
   */
  @NonNull
  @Override
  public String toString() {
    if (code != null) return code.toString();
    else return "";
  }
}
