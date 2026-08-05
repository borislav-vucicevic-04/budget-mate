package com.borislavvucicevic.budgetmate.services;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.borislavvucicevic.budgetmate.enums.Locale;

/**
 * Provides utility methods for managing the application's language settings.
 *
 * <p>This service uses AndroidX per-app language APIs to apply a supported
 * application locale, restore the system language, and retrieve the currently
 * selected application language.</p>
 *
 * <p>The class cannot be instantiated because all operations are exposed
 * through static methods.</p>
 */
public final class LocalisationService {

  /**
   * Prevents instantiation of this utility class.
   */
  private LocalisationService() {
    throw new UnsupportedOperationException(
            "LocalisationService cannot be instantiated."
    );
  }

  /**
   * Sets the application's language to the specified locale.
   *
   * <p>The language change is applied through
   * {@link AppCompatDelegate#setApplicationLocales(LocaleListCompat)}.</p>
   *
   * @param locale the supported application locale to apply; must not be
   *               {@code null}
   */
  public static void setLanguage(@NonNull Locale locale) {
    LocaleListCompat locales =
            LocaleListCompat.forLanguageTags(locale.getLocaleCode());

    AppCompatDelegate.setApplicationLocales(locales);
  }

  /**
   * Restores the application's language to the language configured on the
   * user's device.
   *
   * <p>Passing an empty locale list removes the application's custom language
   * preference and allows Android to use the system locale.</p>
   */
  public static void useSystemLanguage() {
    AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.getEmptyLocaleList()
    );
  }

  /**
   * Returns the language tag of the currently selected application locale.
   *
   * <p>For example, this method may return {@code "en"}, {@code "sr"},
   * {@code "es"}, or {@code "de"}. If the application does not have an
   * explicitly selected locale, an empty string is returned.</p>
   *
   * @return the current application language tag, or an empty string if no
   *         application-specific locale is set
   */
  @NonNull
  public static String getLanguage() {
    LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();

    java.util.Locale currentLocale =
            !locales.isEmpty() ? locales.get(0) : null;

    return currentLocale != null ? currentLocale.toLanguageTag() : "";
  }
}