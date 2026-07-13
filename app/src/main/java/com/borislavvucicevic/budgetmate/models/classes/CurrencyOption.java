package com.borislavvucicevic.budgetmate.models.classes;

import androidx.annotation.NonNull;

import com.borislavvucicevic.budgetmate.models.enums.CurrencyCode;

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
   * Retrieves the raw currency code as a string for data processing or backend storage.
   * <p>
   * This method evaluates the underlying currency enumeration: if a valid code is configured,
   * its exact textual representation is returned; otherwise, it yields an empty fallback
   * string sequence to safeguard against null pointer vulnerabilities.
   * </p>
   *
   * @return A non-null {@link String} matching the currency identifier, or an empty string.
   */
  public String getCodeAsString() {
    if (code != null) return code.toString();
    else return "";
  }

  /**
   * Returns the user-friendly presentation text, used directly by UI layout adapters to render content.
   * <p>
   * This override proxies the underlying display name properties. It allows standard Android UI components,
   * such as {@link android.widget.Spinner Spinners}, to automatically discover and render the localized
   * descriptive label within dropdown element listings without requiring a specialized view wrapper.
   * </p>
   *
   * @return A {@link String} containing the descriptive option text intended for human view visibility.
   */
  @NonNull
  @Override
  public String toString() {
    return this.getDisplayName();
  }
}