package com.borislavvucicevic.budgetmate.services;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.adapters.LocaleAdapter;
import com.borislavvucicevic.budgetmate.enums.Locale;
import com.borislavvucicevic.budgetmate.options.LocaleOption;

import java.util.ArrayList;
import java.util.List;

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
  /**
   * Creates and returns an adapter containing all languages supported by the
   * application.
   *
   * <p>Each supported language is represented by a {@link LocaleOption}
   * containing its display name, corresponding {@link Locale} value, and flag
   * drawable resource.</p>
   *
   * <p>The returned adapter can be assigned to an
   * {@link android.widget.AdapterView}, such as a
   * {@link android.widget.Spinner}, to display the available language
   * options.</p>
   *
   * @param context the context used by the adapter to inflate views and access
   *                application resources; must not be {@code null}
   * @return a locale adapter containing the supported application languages
   */
  @NonNull
  public static LocaleAdapter getLocaleAdapter(@NonNull Context context) {
    List<LocaleOption> localeOptions = new ArrayList<>();
    localeOptions.add(new LocaleOption("English", Locale.EN, R.drawable.uk));
    localeOptions.add(new LocaleOption("Srpski", Locale.SR, R.drawable.serbian));
    localeOptions.add(new LocaleOption("Español", Locale.ES, R.drawable.spanish));
    localeOptions.add(new LocaleOption("Deutsch", Locale.DE, R.drawable.german));

    return new LocaleAdapter(context, localeOptions);
  }

  /**
   * Creates and returns a listener that applies a newly selected application
   * language.
   *
   * <p>When an item is selected, the listener retrieves the corresponding
   * {@link LocaleOption} from the parent adapter, extracts its {@link Locale},
   * and applies it through {@link #setLanguage(Locale)}.</p>
   *
   * <p>No action is performed when the adapter has no selected item.</p>
   *
   * @return a listener that handles locale selection changes
   */
  @NonNull
  public static AdapterView.OnItemSelectedListener getLocaleChangeHandler() {
    return new AdapterView.OnItemSelectedListener() {

      /**
       * Applies the locale associated with the selected adapter item.
       *
       * @param parent   the adapter view in which the selection occurred
       * @param view     the selected item view
       * @param position the position of the selected item
       * @param id       the row ID of the selected item
       */
      @Override
      public void onItemSelected(
              AdapterView<?> parent,
              View view,
              int position,
              long id
      ) {
        LocaleOption localeOption =
                (LocaleOption) parent.getItemAtPosition(position);

        Locale locale = localeOption.getLocaleCode();
        LocalisationService.setLanguage(locale);
      }

      /**
       * Called when the adapter view has no selected item.
       *
       * <p>This implementation intentionally performs no action.</p>
       *
       * @param parent the adapter view whose selection was cleared
       */
      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        // No action required.
      }
    };
  }

  /**
   * Creates a locale option representing the application's currently selected
   * language.
   *
   * <p>The current application language tag is retrieved through
   * {@link #getLanguage()} and converted to a supported {@link Locale} using
   * {@link Locale#parse(String)}.</p>
   *
   * <p>The returned option contains an empty display title and uses the UK flag
   * drawable as its icon.</p>
   *
   * @return a locale option representing the currently selected application
   *         language
   */
  @NonNull
  public static LocaleOption getCurrentLocaleOption() {
    Locale locale = Locale.parse(LocalisationService.getLanguage());
    return new LocaleOption("", locale, R.drawable.uk);
  }

  /**
   * Configures the supplied locale-selection spinner and selects the
   * application's currently active locale.
   *
   * <p>The method assigns the locale adapter provided by
   * {@link LocalisationService#getLocaleAdapter(Context)} to the spinner.
   * It then determines the position of the currently selected locale option
   * and updates the spinner selection accordingly.</p>
   *
   * <p>If the current locale option cannot be found in the adapter, the
   * spinner falls back to the first available item.</p>
   *
   * @param localeSwitch spinner used to display and select application locales
   * @param context context used to create the locale adapter
   */
  public static void setLocaleSwitch(@NonNull Spinner localeSwitch, @NonNull Context context) {
    localeSwitch.setAdapter(LocalisationService.getLocaleAdapter(context));
    int position = ((LocaleAdapter) localeSwitch.getAdapter()).getPosition(LocalisationService.getCurrentLocaleOption());
    localeSwitch.setSelection(Math.max(position, 0));
  }
}