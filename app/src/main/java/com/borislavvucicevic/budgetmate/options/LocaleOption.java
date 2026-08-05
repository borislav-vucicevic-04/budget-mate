package com.borislavvucicevic.budgetmate.options;

import androidx.annotation.Nullable;

import com.borislavvucicevic.budgetmate.enums.Locale;

/**
 * Represents a selectable language option displayed in the application's
 * language-selection interface.
 *
 * <p>Each option contains:
 * <ul>
 *   <li>a user-visible language title,</li>
 *   <li>a {@link Locale} value used to identify the language, and</li>
 *   <li>a drawable resource ID representing the language icon or flag.</li>
 * </ul>
 *
 * <p>Two {@code LocaleOption} instances are considered equal when they have
 * the same {@link Locale} value. The title and icon resource ID are not used
 * when comparing instances.
 */
public class LocaleOption {
  private final String title;
  private final Locale localeCode;
  private final int iconResId;

  /**
   * Creates a new language option.
   *
   * @param title      the user-visible name of the language, such as
   *                   {@code "English"} or {@code "Deutsch"}
   * @param localeCode the {@link Locale} value representing the language
   * @param iconResId  the drawable resource ID of the language icon or flag
   */
  public LocaleOption(String title, Locale localeCode, int iconResId) {
    this.title = title;
    this.localeCode = localeCode;
    this.iconResId = iconResId;
  }

  /**
   * Returns the user-visible title of this language option.
   *
   * @return the language title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Returns the locale associated with this language option.
   *
   * @return the locale value used to identify the selected language
   */
  public Locale getLocaleCode() {
    return localeCode;
  }

  /**
   * Returns the drawable resource ID of the language icon.
   *
   * @return the icon drawable resource ID
   */
  public int getIconResId() {
    return iconResId;
  }

  /**
   * Compares this option with another object.
   *
   * <p>Two {@code LocaleOption} objects are considered equal when they contain
   * the same locale code. Their titles and icon resource IDs are ignored.
   *
   * @param obj the object to compare with this option
   * @return {@code true} if both objects represent the same locale;
   *         otherwise {@code false}
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    LocaleOption other = (LocaleOption) obj;
    return localeCode != null && localeCode.equals(other.localeCode);
  }
}