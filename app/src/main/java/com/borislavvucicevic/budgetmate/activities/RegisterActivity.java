package com.borislavvucicevic.budgetmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.models.classes.UserProfile;
import com.borislavvucicevic.budgetmate.models.exceptions.AuthException;
import com.borislavvucicevic.budgetmate.models.enums.CurrencyCode;
import com.borislavvucicevic.budgetmate.models.classes.CurrencyOption;
import com.borislavvucicevic.budgetmate.models.enums.FirebaseAuthErrorCodes;
import com.borislavvucicevic.budgetmate.models.exceptions.ValidationException;
import com.borislavvucicevic.budgetmate.services.AuthService;
import com.borislavvucicevic.budgetmate.services.DatabaseService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {
  public static final String REGISTER_ACTIVITY = "REGISTER_ACTIVITY";
  private EditText etFullName, etEmail, etPassword, etRepeatPassword;
  private TextView tvErrorWrapper;
  private Spinner spHomeCurrency;
  private ProgressBar progressBar;
  private AuthService authService;
  private DatabaseService databaseService;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_register);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_register), (v, insets) -> {
      int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
      v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), imeBottom);
      return insets;
    });
    // Setting up the currency spinner
    this.setupCurrencySpinner();
    // creating an instance of AuthService
    authService = new AuthService();
    databaseService = new DatabaseService();
    // getting widgets
    etFullName = findViewById(R.id.etFullName);
    etEmail = findViewById(R.id.etEmail);
    etPassword = findViewById(R.id.etPassword);
    etRepeatPassword = findViewById(R.id.etRepeatPassword);
    spHomeCurrency = findViewById(R.id.spReportType);
    tvErrorWrapper = findViewById(R.id.tvErrorWrapper);
    progressBar = findViewById(R.id.progressBar);
    Button btnRegister = findViewById(R.id.btnRegister);
    TextView tvLoginLink = findViewById(R.id.tvLoginLink);

    // setting event handlers
    btnRegister.setOnClickListener(v -> this.handleRegistration());
    tvLoginLink.setOnClickListener(v -> this.handleLoginLink());
  }

  /**
   * Initializes and configures the currency selection spinner.
   * This method creates a list of supported currency options, including a default
   * home currency and specific international currencies (BAM, RSD, EUR, USD).
   * It binds this data to an {@link android.widget.ArrayAdapter} using default
   * Android spinner layouts and attaches the adapter to the {@code spHomeCurrency} view component.
   * */
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
    adapter.setDropDownViewResource(
            R.layout.spinner_layout
    );

    Spinner spinner = findViewById(R.id.spReportType);
    spinner.setAdapter(adapter);
  }

  /**
   * Initiates the asynchronous user registration process.
   * <p>
   * This method extracts the user inputs from the form fields, activates the
   * background progress bar indicator, and offloads processing to a single-thread background
   * executor. On the background thread, it performs input validation and makes an
   * authentication API call to create the account. Results and encountered exceptions
   * are subsequently piped back to the main UI thread via dedicated handler methods.
   * </p>
   */
  private void handleRegistration() {
    // getting form values
    String fullName = etFullName.getText().toString().trim();
    String email = etEmail.getText().toString().trim();
    String password = etPassword.getText().toString().trim();
    String homeCurrency = ((CurrencyOption) spHomeCurrency.getSelectedItem()).getCodeAsString();
    progressBar.setVisibility(View.VISIBLE);

    Executors.newSingleThreadExecutor().execute(() -> {
      try {
        // validating user inputs
        this.validateForm();
        String uid = authService.createUserAccount(email, password);
        authService.sendVerificationEmail();
        databaseService.createUserProfile(uid, new UserProfile(fullName, email, CurrencyCode.valueOf(homeCurrency)));
        // if everything went without throwing an exception and account has been created successfully
        runOnUiThread(this::handleSuccess);
      } catch(ValidationException exception) {
        runOnUiThread(() -> this.handleException(exception));
      } catch (AuthException exception) {
        runOnUiThread(() -> this.handleException(exception));
      } catch (Exception exception) {
        runOnUiThread(() -> this.handleException(exception));
      }
    });
  }

  /**
   * Handles the successful completion of the registration process.
   * <p>
   * This method displays a brief success toast message to the user and
   * navigates the application from the registration screen to login page.
   * </p>
   */
  private void handleSuccess() {
    Toast.makeText(
            RegisterActivity.this,
            getString(R.string.register_success),
            Toast.LENGTH_SHORT
    ).show();
    this.handleLoginLink();
  }

  /**
   * Handles validation failures triggered during the registration input check.
   * <p>
   * This method updates the UI by attaching an error message directly to the invalid
   * input field (if a view ID is provided), displaying a general error text wrapper,
   * showing a toast notification, and hiding the active progress bar. It also logs
   * the exception details for debugging.
   * </p>
   *
   * @param exception The {@link ValidationException} containing the validation failure
   *                  details, the error message, and the target view ID.
   */
  private void handleException(ValidationException exception) {
    if(exception.getViewID() != null) {
      ((EditText) findViewById(exception.getViewID())).setError(exception.getMessage());
    }
    Log.e(REGISTER_ACTIVITY, exception.getMessage(), exception);
    tvErrorWrapper.setVisibility(TextView.VISIBLE);
    tvErrorWrapper.setText(exception.getMessage());
    Toast.makeText(
            RegisterActivity.this,
            exception.getMessage(),
            Toast.LENGTH_SHORT
    ).show();
    progressBar.setVisibility(View.INVISIBLE);
  }

  /**
   * Handles authentication errors returned by the Firebase backend during registration.
   * <p>
   * This method parses the specific Firebase error code to map it to a localized,
   * user-friendly error message (such as invalid email or email already in use). It then
   * displays this message in the error text wrapper and a toast notification, hides the
   * progress bar, and logs the original stack trace.
   * </p>
   *
   * @param exception The {@link AuthException} thrown by the authentication service
   *                  containing the specific error code.
   */
  private void handleException(AuthException exception) {
    String message = "";
    FirebaseAuthErrorCodes code = FirebaseAuthErrorCodes.parse(exception.getErrorCode());
    // extracting localized message shown to user
    switch (code) {
      case ERROR_INVALID_EMAIL: message = getString(R.string.error_invalid_email); break;
      case ERROR_EMAIL_ALREADY_IN_USE: message = getString(R.string.error_email_already_in_use); break;
      default: message = exception.getMessage();
    }
    // logging and displaying the message
    Log.e(REGISTER_ACTIVITY, exception.getMessage(), exception);
    tvErrorWrapper.setVisibility(TextView.VISIBLE);
    tvErrorWrapper.setText(message);
    Toast.makeText(
            RegisterActivity.this,
            message,
            Toast.LENGTH_SHORT
    ).show();
    progressBar.setVisibility(View.INVISIBLE);
  }

  /**
   * Serves as a fallback handler for any generic or unhandled exceptions during registration.
   * <p>
   * This method catches any standard exceptions and logs the specific error details.
   * It surfaces the explicit exception message via the error text wrapper, but displays
   * a generic, localized error message to the user via a toast notification. It also
   * ensures the loading progress bar is hidden.
   * </p>
   *
   * @param exception The generic {@link Exception} encountered during execution.
   */
  private void handleException(Exception exception) {
    Log.e(REGISTER_ACTIVITY, exception.getMessage(), exception);
    tvErrorWrapper.setVisibility(TextView.VISIBLE);
    tvErrorWrapper.setText(exception.getMessage());
    Toast.makeText(
            RegisterActivity.this,
            getString(R.string.error_general),
            Toast.LENGTH_SHORT
    ).show();
    progressBar.setVisibility(View.INVISIBLE);
  }

  /**
   * Navigates the user from the current registration screen to the login screen.
   * <p>
   * This method is triggered when the user clicks the login redirection link or button.
   * It initializes and starts an intent to launch the {@link LoginActivity}.
   * </p>
   */
  private void handleLoginLink() {
    startActivity(new Intent(getApplicationContext(), LoginActivity.class));
    finish();
  }

  /**
   * Validates the user input fields within the registration form.
   * This method extracts data from the full name, email, password, and repeat password
   * input fields, as well as the home currency selection spinner. It performs presence
   * and integrity constraints checking sequentially. If any field fails its validation rule,
   * the method immediately stops execution and routes the error message and failing view ID
   * via a custom exception.
   *
   * @throws ValidationException If any input field is empty, if the password is under 6 characters,
   * or if the confirmation password does not match the chosen password.
   * */
  private void validateForm() throws ValidationException {
    String fullName = etFullName.getText().toString().trim();
    String email = etEmail.getText().toString().trim();
    String password = etPassword.getText().toString();
    String repeatPassword = etRepeatPassword.getText().toString();
    String homeCurrency = ((CurrencyOption) spHomeCurrency.getSelectedItem()).getCodeAsString();
    if(fullName.isEmpty()) throw new ValidationException(getString(R.string.full_name_required), R.id.etFullName);
    if(email.isEmpty()) throw new ValidationException(getString(R.string.email_required), R.id.etEmail);
    if(homeCurrency.isEmpty()) throw new ValidationException(getString(R.string.home_currency_required), null);
    if(password.isEmpty()) throw new ValidationException(getString(R.string.password_required), R.id.etPassword);
    if(password.length() < 6) throw new ValidationException(getString(R.string.password_too_short), R.id.etPassword);
    if(repeatPassword.isEmpty()) throw new ValidationException(getString(R.string.repeat_password_required), R.id.etRepeatPassword);
    if(!repeatPassword.equals(password)) throw new ValidationException(getString(R.string.password_mismatch), R.id.etRepeatPassword);
  }
}