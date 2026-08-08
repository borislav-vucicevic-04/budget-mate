package com.borislavvucicevic.budgetmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.borislavvucicevic.budgetmate.MainActivity;
import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.TemplateActivity;
import com.borislavvucicevic.budgetmate.exceptions.AuthException;
import com.borislavvucicevic.budgetmate.enums.FirebaseAuthErrorCodes;
import com.borislavvucicevic.budgetmate.exceptions.ValidationException;
import com.borislavvucicevic.budgetmate.services.LocalisationService;

/**
 * Activity responsible for authenticating users in the BudgetMate application.
 *
 * <p>This activity provides the user interface and functionality required
 * for signing in to an existing BudgetMate account. It allows users to enter
 * their email address and password, submit their credentials, request a
 * password reset, resend an email verification message, or navigate to the
 * registration screen.</p>
 *
 * <p>Authentication operations are executed on background threads to avoid
 * blocking the Android UI thread. Results are returned to the main thread
 * before updating the user interface.</p>
 *
 * <p>If an authenticated user opens this activity, the user is immediately
 * redirected to {@link MainActivity}.</p>
 *
 * <p>The activity also supports application language changes through the
 * locale selector inherited from {@link TemplateActivity}.</p>
 *
 * @see TemplateActivity
 * @see MainActivity
 * @see RegisterActivity
 * @see AuthException
 */
public class LoginActivity extends TemplateActivity {

  /**
   * Input field used to enter the user's email address.
   */
  private EditText etEmail;

  /**
   * Input field used to enter the user's password.
   */
  private EditText etPassword;

  /**
   * Link displayed when the user needs to verify their email address.
   *
   * <p>The link allows the user to request another email verification
   * message.</p>
   */
  private TextView tvVerifyEmailLink;

  /**
   * Link used to initiate the password reset process.
   */
  private TextView tvPasswordResetLink;

  /**
   * Link used to navigate from the login screen to the registration screen.
   */
  private TextView tvRegisterLink;

  /**
   * Button used to submit the login form.
   */
  private Button btnLogin;

  private String email;

  private String password;

  /**
   * Called when the login activity is first created.
   *
   * <p>This method enables edge-to-edge rendering and configures the root
   * view to account for the on-screen keyboard using window insets.</p>
   *
   * <p>It also checks whether a user is already authenticated. If a logged-in
   * user is detected, the application redirects directly to
   * {@link MainActivity} and closes the login activity.</p>
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
            findViewById(R.id.activityLogin),
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

    // Check if the user is already logged in.
    // If true, redirect directly to the main activity.
    if (authService.isSignedIn()) {
      startActivity(
              new Intent(getApplicationContext(), MainActivity.class)
      );
      finish();
    }
  }

  /**
   * Returns the layout resource used by this activity.
   *
   * <p>The returned resource is used by {@link TemplateActivity} when
   * initializing the activity interface.</p>
   *
   * @return resource identifier of the login activity layout
   */
  @Override
  protected int getLayoutID() {
    return R.layout.activity_login;
  }

  /**
   * Retrieves and stores references to the user-interface widgets
   * contained in the login activity layout.
   *
   * <p>This includes the email and password input fields, login button,
   * authentication-related links, progress indicator, error wrapper,
   * and locale selector.</p>
   */
  @Override
  protected void grabWidgets() {
    etEmail = findViewById(R.id.etEmail);
    etPassword = findViewById(R.id.etPassword);
    tvErrorWrapper = findViewById(R.id.tvErrorWrapper);
    tvVerifyEmailLink = findViewById(R.id.tvVerifyEmailLink);
    progressBar = findViewById(R.id.progressBar);
    btnLogin = findViewById(R.id.btnLogin);
    tvPasswordResetLink = findViewById(R.id.tvPasswordResetLink);
    tvRegisterLink = findViewById(R.id.tvRegisterLink);
    localeSwitch = findViewById(R.id.localeSwitch);
  }

  /**
   * Registers event listeners for the interactive widgets on the login screen.
   *
   * <p>The listeners handle login attempts, registration navigation,
   * email verification requests, password reset requests, and application
   * language changes.</p>
   */
  @Override
  protected void setListeners() {
    btnLogin.setOnClickListener(v -> this.btnLoginClickHandler());
    tvRegisterLink.setOnClickListener(v -> this.openActivity(
            RegisterActivity.class,
            true,
            true
    ));
    tvVerifyEmailLink.setOnClickListener(v -> this.handleVerifyEmailLink());
    tvPasswordResetLink.setOnClickListener(this::handleResetPasswordLink);
    localeSwitch.setOnItemSelectedListener(LocalisationService.getLocaleChangeHandler());
  }

  @Override
  protected void grabValues() {
    email = etEmail.getText().toString().trim();
    password = etPassword.getText().toString();
  }

  /**
   * Initiates the asynchronous user login process.
   *
   * <p>The current values are retrieved from the login form before the loading
   * indicator is displayed. The login operation is then delegated to a
   * background thread to avoid blocking the Android UI thread.</p>
   *
   * <p>The actual validation and authentication logic is handled by
   * {@link #handleLogin()}.</p>
   */
  private void btnLoginClickHandler() {
    this.grabValues();
    this.toggleProgressBarVisibility();
    this.doInBackground(this::handleLogin);
  }

  /**
   * Validates the login form and attempts to authenticate the user.
   *
   * <p>The entered form values are first validated. If validation succeeds,
   * the authentication service attempts to sign in the user using the supplied
   * email address and password.</p>
   *
   * <p>After successful authentication, {@link #handleSuccess()} is invoked on
   * the main UI thread.</p>
   *
   * <p>If an error occurs, the corresponding exception handler is invoked on
   * the main UI thread:</p>
   *
   * <ul>
   *   <li>{@link ValidationException} is handled as a form validation error.</li>
   *   <li>{@link AuthException} is handled as an authentication error.</li>
   *   <li>Any other {@link Exception} is handled as a general application error.</li>
   * </ul>
   *
   * <p>This method is intended to be executed on a background thread.</p>
   */
  private void handleLogin() {
    try {
      this.validateForm();
      authService.signIn(email, password);
      runOnUiThread(this::handleSuccess);
    } catch (ValidationException exception) {
      runOnUiThread(() -> this.handleException(
              exception,
              LoginActivity.class
      ));
    } catch (AuthException exception) {
      runOnUiThread(() -> this.handleException(exception));
    } catch (Exception exception) {
      runOnUiThread(() -> this.handleException(
              exception,
              getString(R.string.error_general),
              LoginActivity.class
      ));
    }
  }

  /**
   * Completes the login process after successful authentication.
   *
   * <p>A success notification is displayed before the user is redirected
   * to {@link MainActivity}. The current login activity is then finished
   * so that it is removed from the activity back stack.</p>
   */
  private void handleSuccess() {
    showToast(getString(R.string.login_success));

    startActivity(
            new Intent(getApplicationContext(), MainActivity.class)
    );

    finish();
  }

  /**
   * Handles authentication errors returned by the authentication service
   * during the login process.
   *
   * <p>The error code contained within the supplied {@link AuthException}
   * is converted into a {@link FirebaseAuthErrorCodes} value and mapped to
   * an appropriate localized message.</p>
   *
   * <p>If the user's email has not yet been verified, the email
   * verification link is also made visible so that another verification
   * email can be requested.</p>
   *
   * <p>The generated error message and original exception are then passed
   * to the generic exception handler inherited from
   * {@link TemplateActivity}.</p>
   *
   * @param exception authentication exception containing the Firebase
   *                  authentication error code
   */
  private void handleException(@NonNull AuthException exception) {
    String message;

    FirebaseAuthErrorCodes code = FirebaseAuthErrorCodes.parse(exception.getErrorCode());

    // Extract localized message shown to the user.
    if (code != null) {
      switch (code) {
        case ERROR_INVALID_EMAIL:
          message = getString(R.string.error_invalid_email);
          break;

        case ERROR_INVALID_CREDENTIAL:
          message = getString(R.string.error_invalid_credential);
          break;

        case ERROR_TOO_MANY_REQUESTS:
          message = getString(R.string.error_too_many_requests);
          break;

        case ERROR_EMAIL_NOT_VERIFIED:
          message = getString(R.string.error_email_not_verified);
          tvVerifyEmailLink.setVisibility(View.VISIBLE);
          break;

        default:
          message = getString(R.string.error_general);
      }
    } else {
      message = getString(R.string.error_general);
    }

    // Log and display the message.
    this.handleException(exception, message, LoginActivity.class);
  }

  /**
   * Initiates the asynchronous email verification process.
   *
   * <p>The progress indicator is displayed before the authentication
   * service attempts to send a verification email on a background thread.</p>
   *
   * <p>If the operation succeeds, the verification link text is changed
   * to indicate that the email may be resent, the progress indicator is
   * hidden, and a confirmation message is displayed.</p>
   *
   * <p>Authentication and unexpected errors are forwarded to their
   * corresponding exception handlers on the main UI thread.</p>
   */
  private void handleVerifyEmailLink() {
    toggleProgressBarVisibility();

    this.doInBackground(() -> {
      try {
        authService.sendVerificationEmail();
        runOnUiThread(() -> {
          tvVerifyEmailLink.setText(getText(R.string.resend_verification_email));
          toggleProgressBarVisibility();
          showToast(getString(R.string.verification_email_sent));
        });
      } catch (AuthException exception) {
        runOnUiThread(() -> this.handleException(exception));
      } catch (Exception exception) {
        runOnUiThread(() -> handleException(
                        exception,
                        getString(R.string.error_general),
                        LoginActivity.class
        ));
      }
    });
  }

  /**
   * Displays a dialog that allows the user to request a password reset.
   *
   * <p>The dialog contains an email input field and positive and negative
   * action buttons. When the user confirms the request, the entered email
   * address is passed to {@link #handleResetPassword(String)}.</p>
   *
   * <p>Selecting the negative action closes the dialog without performing
   * any additional operation.</p>
   *
   * @param v view that triggered the password reset action; its context
   *          is used when creating the dialog
   */
  private void handleResetPasswordLink(@NonNull View v) {
    EditText resetMail = new EditText(v.getContext());

    AlertDialog.Builder passwordResetDialog =
            new AlertDialog.Builder(v.getContext());

    passwordResetDialog.setTitle(
            getString(R.string.password_reset_dialog_title)
    );

    passwordResetDialog.setMessage(
            getString(R.string.password_reset_dialog_message)
    );

    passwordResetDialog.setView(resetMail);

    passwordResetDialog.setPositiveButton(
            getString(R.string.password_reset_dialog_positive),
            (dialog, which) ->
                    this.handleResetPassword(
                            resetMail.getText().toString().trim()
                    )
    );

    passwordResetDialog.setNegativeButton(
            getString(R.string.password_reset_dialog_negative),
            (dialog, which) -> {
              // Do nothing.
            }
    );

    passwordResetDialog.show();
  }

  /**
   * Sends a password reset email to the supplied email address.
   *
   * <p>The password reset operation is executed on a background thread
   * using the authentication service. If the request succeeds, a
   * confirmation message is displayed to the user on the main UI thread.</p>
   *
   * <p>Authentication-related and unexpected exceptions are forwarded to
   * the appropriate exception handlers.</p>
   *
   * @param email email address to which the password reset message
   *              should be sent
   */
  private void handleResetPassword(String email) {
    this.doInBackground(() -> {
      try {
        authService.sendPasswordResetEmail(email);
        runOnUiThread(() -> showToast(getString(R.string.password_reset_dialog_success)));
      } catch (AuthException exception) {
        runOnUiThread(() -> this.handleException(exception));

      } catch (Exception exception) {
        runOnUiThread(() -> this.handleException(
                        exception,
                        getString(R.string.error_general),
                        LoginActivity.class
        ));
      }
    });
  }

  /**
   * Validates the required fields of the login form.
   *
   * <p>The method checks whether an email address and password have been
   * supplied. Validation is performed sequentially, and execution stops
   * as soon as an invalid field is detected.</p>
   *
   * <p>A {@link ValidationException} contains both the localized validation
   * message and the ID of the input field that caused the validation
   * failure.</p>
   *
   * @throws ValidationException if the email or password field is empty
   */
  private void validateForm() throws ValidationException {
    if (email.isEmpty()) {throw new ValidationException(
              getString(R.string.email_required),
              R.id.etEmail
      );
    }
    if (password.isEmpty()) {
      throw new ValidationException(
              getString(R.string.password_required),
              R.id.etPassword
      );
    }
  }
}