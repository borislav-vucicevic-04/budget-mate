package com.borislavvucicevic.budgetmate.enums;

/**
 * Represents the languages supported by the application.
 *
 * <p>Each enum constant contains the language code used to identify the
 * corresponding Android locale and localized string resources.</p>
 *
 * <p>For example, {@link #DE} uses the locale code {@code "de"} and
 * corresponds to resources stored in the {@code values-de} directory.</p>
 */
public enum Locale {

  /** English language using the locale code {@code "en"}. */
  EN("en"),

  /** Serbian language using the locale code {@code "sr"}. */
  SR("sr"),

  /** Spanish language using the locale code {@code "es"}. */
  ES("es"),

  /** German language using the locale code {@code "de"}. */
  DE("de");

  /** The language code associated with this locale. */
  private final String localeCode;

  /**
   * Creates a supported locale with the specified language code.
   *
   * @param localeCode the language code associated with the locale,
   *                   such as {@code "en"}, {@code "sr"}, {@code "es"},
   *                   or {@code "de"}
   */
  Locale(String localeCode) {
    this.localeCode = localeCode;
  }

  /**
   * Returns the language code associated with this locale.
   *
   * @return the locale language code
   */
  public String getLocaleCode() {
    return localeCode;
  }
}