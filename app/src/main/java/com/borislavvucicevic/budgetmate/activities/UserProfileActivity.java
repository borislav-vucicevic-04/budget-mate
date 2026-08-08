package com.borislavvucicevic.budgetmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.TemplateActivity;
import com.borislavvucicevic.budgetmate.enums.CacheKey;
import com.borislavvucicevic.budgetmate.models.UserProfile;
import com.borislavvucicevic.budgetmate.enums.CurrencyCode;
import com.borislavvucicevic.budgetmate.services.CacheService;
import com.borislavvucicevic.budgetmate.services.LocalisationService;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;

import java.util.concurrent.Executors;

/**
 * Activity responsible for displaying the authenticated user's profile
 * information in the BudgetMate application.
 *
 * <p>This activity retrieves the current user's profile from the database
 * and displays their full name, email address, and configured home currency.</p>
 *
 * <p>The user profile is loaded asynchronously on a background thread to
 * prevent database operations from blocking the Android UI thread. After the
 * profile has been retrieved successfully, the user-interface components are
 * updated on the main thread.</p>
 *
 * <p>The stored {@link CurrencyCode} is converted into its corresponding
 * localized currency label before being displayed to the user.</p>
 *
 * <p>The activity also supports changing the application's locale through
 * the locale selector inherited from {@link TemplateActivity}.</p>
 *
 * @see TemplateActivity
 * @see UserProfile
 * @see CurrencyCode
 * @see LocalisationService
 */
public class UserProfileActivity extends TemplateActivity {

  /**
   * Text view used to display the user's full name.
   */
  private TextView tvFullName;

  /**
   * Text view used to display the user's email address.
   */
  private TextView tvEmail;

  /**
   * Text view used to display the localized name of the user's configured
   * home currency.
   */
  private TextView tvHomeCurrency;

  /**
   * Scrollable container holding the user profile information.
   *
   * <p>The container becomes visible after the user profile has been
   * retrieved successfully.</p>
   */
  private ScrollView formWrapper;

  private Button btnLogOut;

  private Button btnDeleteAccount;

  /**
   * Called when the user profile activity is first created.
   *
   * <p>This method enables edge-to-edge rendering and configures the root
   * view to respect Android system-bar insets.</p>
   *
   * <p>The user profile is then loaded asynchronously using a
   * single-thread executor. The background operation is delegated to
   * {@link #loadUserProfile()}.</p>
   *
   * @param savedInstanceState previously saved activity state, or
   *                           {@code null} if the activity is being
   *                           created for the first time
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
      Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
      return insets;
    });

    // executing the heavy task on background process
    Executors.newSingleThreadExecutor().execute(this::loadUserProfile);
  }

  /**
   * Returns the layout resource used by this activity.
   *
   * @return resource identifier of the user profile activity layout
   */
  @Override
  protected int getLayoutID() {
    return R.layout.activity_user_profile;
  }

  /**
   * Retrieves and stores references to the user-interface widgets contained
   * in the user profile activity layout.
   *
   * <p>This includes the text views displaying the user's full name, email
   * address, and home currency, as well as the profile form container,
   * progress indicator, and locale selector.</p>
   */
  @Override
  protected void grabWidgets() {
    tvFullName = findViewById(R.id.tvFullName);
    tvEmail = findViewById(R.id.tvEmail);
    tvHomeCurrency = findViewById(R.id.tvHomeCurrency);
    formWrapper = findViewById(R.id.formWrapper);
    btnLogOut = findViewById(R.id.btnLogOut);
    btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
    progressBar = findViewById(R.id.progressBar);
    localeSwitch = findViewById(R.id.localeSwitch);
  }

  /**
   * Registers event listeners for the interactive controls on the user
   * profile screen.
   *
   * <p>The locale selector is connected to the locale change handler provided
   * by {@link LocalisationService}, allowing the user to change the
   * application's language.</p>
   */
  @Override
  protected void setListeners() {
    btnLogOut.setOnClickListener(v -> this.handleSignOut());
    btnDeleteAccount.setOnClickListener(this::showDeleteAccountConfirmation);
    localeSwitch.setOnItemSelectedListener(LocalisationService.getLocaleChangeHandler());
  }

  /**
   * Retrieves the authenticated user's profile from the database and displays
   * the profile information on the screen.
   *
   * <p>The authenticated user's identifier is obtained from the authentication
   * service and used to retrieve a corresponding {@link UserProfile} from the
   * database.</p>
   *
   * <p>After the profile has been retrieved, the UI is updated on the main
   * thread. The user's configured {@link CurrencyCode} is converted into its
   * localized display value before being assigned to the home-currency text
   * view.</p>
   *
   * <p>The supported currency values are:</p>
   *
   * <ul>
   *   <li>{@link CurrencyCode#BAM}</li>
   *   <li>{@link CurrencyCode#EUR}</li>
   *   <li>{@link CurrencyCode#RSD}</li>
   *   <li>{@link CurrencyCode#USD}</li>
   * </ul>
   *
   * <p>Once all profile information has been displayed, the profile form is
   * made visible and the progress indicator is hidden.</p>
   *
   * <p>If an exception occurs while retrieving or processing the profile,
   * it is forwarded to the generic exception handler inherited from
   * {@link TemplateActivity} on the main UI thread.</p>
   */
  private void loadUserProfile() {
    String uid = authService.getUserID();
    try {
      UserProfile userProfile = databaseService.getUserProfile(uid);
      runOnUiThread(() -> {
        // parsing homeCurrency
        CurrencyCode currencyCode = userProfile.getHomeCurrency();
        String homeCurrency = "";

        switch (currencyCode) {
          case BAM: homeCurrency = getString(R.string.currency_bam); break;
          case EUR: homeCurrency = getString(R.string.currency_eur); break;
          case RSD: homeCurrency = getString(R.string.currency_rsd); break;
          case USD: homeCurrency = getString(R.string.currency_usd); break;
          default: /* DO NOTHING */ break;
        }

        // setting text to text views
        tvFullName.setText(userProfile.getFullName());
        tvEmail.setText(userProfile.getEmail());
        tvHomeCurrency.setText(homeCurrency);
        formWrapper.setVisibility(View.VISIBLE);
        toggleProgressBarVisibility();
      });
    } catch (Exception exception) {
      runOnUiThread(() -> this.handleException(exception, getString(R.string.error_general), UserProfileActivity.class));
    }
  }

  /**
   * Displays a confirmation dialog before deleting the currently authenticated
   * user's account.
   *
   * <p>The dialog prompts the user to enter their password and provides options
   * to either confirm or cancel the account deletion. If the user confirms,
   * the entered password is retrieved, trimmed, and passed to
   * {@link #handleDeleteAccount(String)} to perform the account deletion.</p>
   *
   * <p>If the user cancels the operation, the dialog is dismissed without
   * making any changes to the account.</p>
   *
   * @param v the view used to obtain the context for the password input field;
   *          must not be {@code null}
   */
  private void showDeleteAccountConfirmation(@NonNull View v) {
    EditText etPassword = new EditText(v.getContext());

    int margin = getResources().getDimensionPixelSize(R.dimen.dialog_input_margin);
    int minHeight = getResources().getDimensionPixelSize(R.dimen.dialog_input_min_height);

    // Background
    etPassword.setBackgroundResource(R.drawable.bg_input_field);

    // Text color
    etPassword.setTextColor(ContextCompat.getColor(v.getContext(), R.color.black));

    etPassword.setHintTextColor(ContextCompat.getColor(v.getContext(), R.color.gray));

    // Minimum height: 48dp
    etPassword.setMinHeight(minHeight);

    // Password input
    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

    etPassword.setHint(getString(R.string.password));

    // Container gives the EditText an 8dp margin around it
    FrameLayout container = new FrameLayout(v.getContext());
    container.setPadding(margin, margin, margin, margin);

    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
    );

    container.addView(etPassword, params);

    MaterialAlertDialogBuilder dialog =
            new MaterialAlertDialogBuilder(this, R.style.CustomAlertDialogTheme);

    dialog.setTitle(getString(R.string.delete_account_dialog_title));
    dialog.setMessage(getString(R.string.delete_account_dialog_message));
    dialog.setView(container);
    dialog.setPositiveButton(
            getString(R.string.delete_account_dialog_yes),
            (d, which) -> {
              String password = etPassword.getText().toString().trim();
              handleDeleteAccount(password);
            }
    );
    dialog.setNegativeButton(
            getString(R.string.delete_account_dialog_no),
            (d, which) -> d.dismiss()
    );

    dialog.show();
  }

  /**
   * Handles the user sign-out process.
   *
   * <p>Signs the current user out, clears all locally cached data, and navigates
   * to {@link LoginActivity}. The activity task is cleared so the user cannot
   * return to previously opened authenticated activities by pressing the Back button.</p>
   */
  private void handleSignOut() {
    this.authService.signOut();
    CacheService.clearAll();
    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
  }

  /**
   * Deletes the currently authenticated user's account using the provided password.
   *
   * <p>This method hides the account deletion form, displays a progress indicator,
   * and performs the deletion operation asynchronously on a background thread.
   * It removes the user's profile from the database and then deletes the
   * authentication account.</p>
   *
   * <p>If the deletion succeeds, the user is redirected to {@link LoginActivity}
   * and the existing activity task is cleared. If an error occurs, the form is
   * shown again and the exception is passed to the application's exception
   * handler.</p>
   *
   * @param password the current user's password used to authenticate the account
   *                 deletion request; must not be {@code null}
   */
  private void handleDeleteAccount(@NonNull String password) {
    formWrapper.setVisibility(View.GONE);
    toggleProgressBarVisibility();
    showToast(getString(R.string.deleting_account));
    Executors.newSingleThreadExecutor().execute(() -> {
      try {
        UserProfile userProfile = CacheService.read(CacheKey.USER_PROFILE, UserProfile.class);
        String uid = authService.getUserID();
        String email = userProfile.getEmail();
        databaseService.deleteUserProfile(uid);
        authService.deleteUserAccount(
                email,
                password,
                this.databaseService
        );
        Thread.sleep(2000);
        runOnUiThread(() -> {
          showToast(getString(R.string.delete_account_success));
          Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
          startActivity(intent);
        });
      } catch (Exception exception) {
        runOnUiThread(() -> {
          formWrapper.setVisibility(View.VISIBLE);
          handleException(
                  exception,
                  getString(R.string.error_general),
                  UserProfileActivity.class
          );
        });
      }
    });
  }
}