package com.borislavvucicevic.budgetmate.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.TemplateActivity;
import com.borislavvucicevic.budgetmate.models.UserProfile;
import com.borislavvucicevic.budgetmate.exceptions.AuthException;
import com.borislavvucicevic.budgetmate.enums.CurrencyCode;
import com.borislavvucicevic.budgetmate.options.CurrencyOption;
import com.borislavvucicevic.budgetmate.enums.FirebaseAuthErrorCodes;
import com.borislavvucicevic.budgetmate.exceptions.ValidationException;
import com.borislavvucicevic.budgetmate.services.LocalisationService;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Activity responsible for registering new users in the BudgetMate application.
 *
 * <p>This activity provides the user interface and functionality required
 * for creating a new BudgetMate account. The user must provide their full
 * name, email address, password, password confirmation, and preferred home
 * currency.</p>
 *
 * <p>Before an account is created, the supplied registration data is validated.
 * If validation succeeds, the authentication service creates the user account
 * and sends an email verification message. A corresponding
 * {@link UserProfile} is then created and stored through the database
 * service.</p>
 *
 * <p>Registration operations are executed on a background thread to prevent
 * blocking the Android UI thread. Results and errors are subsequently handled
 * on the main UI thread.</p>
 *
 * <p>The activity also supports changing the application language through
 * the locale selector inherited from {@link TemplateActivity}.</p>
 *
 * @see TemplateActivity
 * @see LoginActivity
 * @see UserProfile
 * @see CurrencyOption
 */
public class RegisterActivity extends TemplateActivity {
  /**
   * Input field used to enter the user's full name.
   */
  private EditText etFullName;

  /**
   * Input field used to enter the user's email address.
   */
  private EditText etEmail;

  /**
   * Input field used to enter the user's password.
   */
  private EditText etPassword;

  /**
   * Input field used to confirm the user's password.
   */
  private EditText etRepeatPassword;

  /**
   * Link used to navigate from the registration screen to the login screen.
   */
  private TextView tvLoginLink;

  /**
   * Spinner used to select the user's preferred home currency.
   */
  private Spinner spHomeCurrency;

  /**
   * Button used to submit the registration form.
   */
  private Button btnRegister;

  /**
   * Holds the value of {@link RegisterActivity#etFullName} widget.
   * */
  private String fullName;

  /**
   * Holds the value of {@link RegisterActivity#etEmail} widget
   * */
  private String email;

  /**
   * Holds the value of {@link RegisterActivity#etPassword} widget
   * */
  private String password;

  /**
   * Holds the value of {@link RegisterActivity#etRepeatPassword} widget
   * */
  private String repeatPassword;

  /**
   * Holds the value of {@link RegisterActivity#spHomeCurrency} widget
   * */
  private String homeCurrency;

  /**
   * Called when the registration activity is first created.
   *
   * <p>This method enables edge-to-edge rendering and configures the root
   * view to account for the on-screen keyboard by applying the appropriate
   * window insets.</p>
   *
   * <p>It also initializes the home-currency selection spinner by calling
   * {@link #setupCurrencySpinner()}.</p>
   *
   * @param savedInstanceState previously saved activity state, or
   *                           {@code null} if the activity is being
   *                           created for the first time
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    EdgeToEdge.enable(this);

    ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.activity_register),
            (v, insets) -> {
              int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;

              v.setPadding(
                      v.getPaddingLeft(),
                      v.getPaddingTop(),
                      v.getPaddingRight(),
                      imeBottom
              );

              return insets;
            }
    );

    // Set up the currency selection spinner.
    this.setupCurrencySpinner();
  }

  /**
   * Returns the layout resource used by this activity.
   *
   * <p>The returned resource is used by {@link TemplateActivity} when
   * initializing the activity interface.</p>
   *
   * @return resource identifier of the registration activity layout
   */
  @Override
  protected int getLayoutID() {
    return R.layout.activity_register;
  }

  /**
   * Retrieves and stores references to the user-interface widgets
   * contained in the registration activity layout.
   *
   * <p>This includes the registration input fields, home-currency spinner,
   * error wrapper, progress indicator, registration button, login link,
   * and locale selector.</p>
   */
  @Override
  protected void grabWidgets() {
    super.grabWidgets();
    etFullName = findViewById(R.id.etFullName);
    etEmail = findViewById(R.id.etEmail);
    etPassword = findViewById(R.id.etPassword);
    etRepeatPassword = findViewById(R.id.etRepeatPassword);
    spHomeCurrency = findViewById(R.id.spReportType);
    btnRegister = findViewById(R.id.btnRegister);
    tvLoginLink = findViewById(R.id.tvLoginLink);
  }

  /**
   * Registers event listeners for the interactive widgets on the
   * registration screen.
   *
   * <p>The registration button initiates account creation, the login link
   * navigates to {@link LoginActivity}, and the locale selector handles
   * application language changes through {@link LocalisationService}.</p>
   */
  @Override
  protected void setListeners() {
    super.setListeners();
    btnRegister.setOnClickListener(v -> this.btnRegisterClickHandler());
    tvLoginLink.setOnClickListener(v -> this.openActivity(
            LoginActivity.class,
            true,
            true
    ));
  }

  /**
   * Grabs values of widgets and stores them in the class fields.
   * */
  @Override
  protected void grabValues() {
    fullName = etFullName.getText().toString().trim();
    email = etEmail.getText().toString().trim();
    password = etPassword.getText().toString();
    repeatPassword = etRepeatPassword.getText().toString();
    homeCurrency = ((CurrencyOption) spHomeCurrency.getSelectedItem()).getCodeAsString();
  }

  /**
   * Initializes and configures the home-currency selection spinner.
   *
   * <p>The method creates a collection of {@link CurrencyOption} objects
   * representing the currencies supported by the application. The first
   * entry acts as the default placeholder, followed by BAM, RSD, EUR,
   * and USD currency options.</p>
   *
   * <p>The currency options are assigned to an {@link ArrayAdapter},
   * which uses the application's custom spinner layout. The configured
   * adapter is then attached to the home-currency spinner.</p>
   *
   * @see CurrencyOption
   * @see CurrencyCode
   */
  private void setupCurrencySpinner() {
    List<CurrencyOption> currencies = new ArrayList<>();

    currencies.add(new CurrencyOption(getString(R.string.home_currency), null));
    currencies.add(new CurrencyOption(getString(R.string.currency_bam), CurrencyCode.BAM));
    currencies.add(new CurrencyOption(getString(R.string.currency_rsd), CurrencyCode.RSD));
    currencies.add(new CurrencyOption(getString(R.string.currency_eur), CurrencyCode.EUR));
    currencies.add(new CurrencyOption(getString(R.string.currency_usd), CurrencyCode.USD));

    ArrayAdapter<CurrencyOption> adapter = new ArrayAdapter<>(
            this,
            R.layout.spinner_layout,
            currencies
    );

    adapter.setDropDownViewResource(R.layout.spinner_layout);
    Spinner spinner = findViewById(R.id.spReportType);
    spinner.setAdapter(adapter);
  }

  /**
   * Handles the registration button click by starting user registration asynchronously.
   *
   * <p>On success, {@link #handleSuccess()} is executed. Authentication errors are
   * handled by {@link #handleAuthException(AuthException)}.</p>
   *
   * @see #doInBackground(Runnable, Runnable, Consumer, Class)
   * @see #handleSuccess()
   * @see #handleAuthException(AuthException)
   */
  private void btnRegisterClickHandler() {
    this.grabValues();
    this.doInBackground(
            this::handleRegistration,
            this::handleSuccess,
            this::handleAuthException,
            AuthException.class
    );
  }

  /**
   * Validates the registration data and creates the user's account and profile.
   *
   * <p>This method is intended to run on a background thread using {@code doInBackground}. It creates the
   * authentication account, sends a verification email, and stores the user profile.</p>
   *
   * @see #doInBackground(Runnable, Runnable, Consumer, Class)
   */
  @WorkerThread
  private void handleRegistration() {
    // Validate user input.
    this.handleValidation();

    // Create authentication account.
    String uid = authService.createUserAccount(email, password);

    // Send account verification email.
    authService.sendVerificationEmail();

    // Create and store the user's application profile.
    databaseService.createUserProfile(
            uid,
            new UserProfile(
                    fullName,
                    email,
                    CurrencyCode.valueOf(homeCurrency)
            )
    );
  }

  /**
   * Handles a successful registration on the UI thread, by redirecting user to the login page.
   *
   * <p>This method is passed to {@code doInBackground} as the {@code whenDone}
   * action and is executed after the background login process completes successfully.</p>
   *
   * @see #doInBackground(Runnable, Runnable, Consumer, Class)
   */
  @UiThread
  private void handleSuccess() {
    this.showToast(getString(R.string.register_success));
    this.openActivity(LoginActivity.class, true, true);
  }

  /**
   * Handles authentication errors raised during the background login process.
   *
   * <p>This method is passed to {@code doInBackground} as the custom handler for
   * {@link AuthException} and is executed on the UI thread when such an exception occurs.</p>
   *
   * <p>The authentication error code is mapped to a localized message and then
   * delegated to the general exception handler. For an unverified email address,
   * the email verification link is also displayed.</p>
   *
   * @param exception the authentication exception to handle
   *
   * @see #doInBackground(Runnable, Runnable, Consumer, Class)
   */
  @UiThread
  private void handleAuthException(@NonNull AuthException exception) {
    String message;
    FirebaseAuthErrorCodes code = FirebaseAuthErrorCodes.parse(exception.getErrorCode());

    // Extract localized message shown to the user.
    switch (code) {
      case ERROR_INVALID_EMAIL:
        message = getString(R.string.error_invalid_email);
        break;

      case ERROR_EMAIL_ALREADY_IN_USE:
        message = getString(R.string.error_email_already_in_use);
        break;

      default:
        message = getString(R.string.error_general);
    }

    // Log and display the error.
    this.handleException(
            exception,
            message,
            RegisterActivity.class
    );
  }

  /**
   * Validates the input fields contained within the registration form.
   *
   * <p>The method verifies that the user has supplied a full name, email
   * address, home currency, password, and password confirmation. It also
   * verifies that the password contains at least six characters and that
   * the repeated password matches the original password.</p>
   *
   * <p>Validation is performed sequentially. As soon as an invalid value
   * is detected, a {@link ValidationException} is thrown containing a
   * localized error message and, where applicable, the ID of the
   * user-interface component responsible for the validation failure.</p>
   *
   * @throws ValidationException if a required field is empty, the password
   *                             contains fewer than six characters, or the
   *                             repeated password does not match the
   *                             original password
   */
  @WorkerThread
  private void handleValidation() throws ValidationException {
    if (fullName.isEmpty()) {
      throw new ValidationException(
              getString(R.string.full_name_required),
              R.id.etFullName
      );
    }

    if (email.isEmpty()) {
      throw new ValidationException(
              getString(R.string.email_required),
              R.id.etEmail
      );
    }

    if (homeCurrency.isEmpty()) {
      throw new ValidationException(
              getString(R.string.home_currency_required),
              null
      );
    }

    if (password.isEmpty()) {
      throw new ValidationException(
              getString(R.string.password_required),
              R.id.etPassword
      );
    }

    if (password.length() < 6) {
      throw new ValidationException(
              getString(R.string.password_too_short),
              R.id.etPassword
      );
    }

    if (repeatPassword.isEmpty()) {
      throw new ValidationException(
              getString(R.string.repeat_password_required),
              R.id.etRepeatPassword
      );
    }

    if (!repeatPassword.equals(password)) {
      throw new ValidationException(
              getString(R.string.password_mismatch),
              R.id.etRepeatPassword
      );
    }
  }
}