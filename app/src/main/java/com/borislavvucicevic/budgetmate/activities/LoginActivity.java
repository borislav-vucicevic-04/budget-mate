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

import java.util.concurrent.Executors;

public class LoginActivity extends TemplateActivity {
  private EditText etEmail, etPassword;
  private TextView tvVerifyEmailLink, tvPasswordResetLink, tvRegisterLink;
  private Button btnLogin;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activityLogin), (v, insets) -> {
      int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
      v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), imeBottom);
      return insets;
    });

    // checking if the user is already logged in. If true, redirect them to the
    // main activity immediately

    if(authService.isLoggedIn()) {
      startActivity(new Intent(getApplicationContext(), MainActivity.class));
      finish();
    }
  }

  @Override
  protected int getLayoutID() {
    return R.layout.activity_login;
  }

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

  @Override
  protected void setListeners() {
    btnLogin.setOnClickListener(v -> this.handleLogin());
    tvRegisterLink.setOnClickListener(v -> this.handleRegisterLink());
    tvVerifyEmailLink.setOnClickListener(v -> this.handleVerifyEmailLink());
    tvPasswordResetLink.setOnClickListener(this::handleResetPasswordLink);
    localeSwitch.setOnItemSelectedListener(LocalisationService.getLocaleChangeHandler());
  }

  /**
   * Initiates the asynchronous user login process.
   * <p>
   * This method extracts the user inputs from the form fields, activates the
   * background progress bar indicator, and offloads processing to a single-thread background
   * executor. On the background thread, it performs input validation and makes an
   * authentication API call to sign in the user. Results and encountered exceptions
   * are subsequently piped back to the main UI thread via dedicated handler methods.
   * </p>
   */
  private void handleLogin() {
    String email = etEmail.getText().toString().trim();
    String password = etPassword.getText().toString();

    toggleProgressBarVisibility();

    Executors.newSingleThreadExecutor().execute(() -> {
      try {
        this.validateForm();
        authService.signInUser(email, password);
        runOnUiThread(this::handleSuccess);
      } catch (ValidationException exception)  {
        runOnUiThread(() -> this.handleException(exception, LoginActivity.class));
      } catch (AuthException exception){
        runOnUiThread(() -> this.handleException(exception));
      } catch(Exception exception) {
        runOnUiThread(() -> this.handleException(exception, getString(R.string.error_general), LoginActivity.class));
      }
    });
  }

  /**
   * Finalizes the authentication process upon a successful user operation.
   * <p>
   * This method displays a brief success toast notification to the user,
   * routes the application flow to the main activity screen, and terminates
   * the current activity to remove it from the back stack.
   * </p>
   */
  private void handleSuccess() {
    showToast(getString(R.string.login_success));
    startActivity(new Intent(getApplicationContext(), MainActivity.class));
    finish();
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
  private void handleException(@NonNull AuthException exception) {
    String message;
    FirebaseAuthErrorCodes code = FirebaseAuthErrorCodes.parse(exception.getErrorCode());
    // extracting localized message shown to user
    if(code != null) {
      switch (code) {
        case ERROR_INVALID_EMAIL: message = getString(R.string.error_invalid_email); break;
        case ERROR_INVALID_CREDENTIAL: message = getString(R.string.error_invalid_credential); break;
        case ERROR_TOO_MANY_REQUESTS: message = getString(R.string.error_too_many_requests); break;
        case ERROR_EMAIL_NOT_VERIFIED:
          message = getString(R.string.error_email_not_verified);
          tvVerifyEmailLink.setVisibility(View.VISIBLE);
          break;
        default: message = getString(R.string.error_general);
      }
    }
    else {
      message = getString(R.string.error_general);
    }
    // logging and displaying the message
   this.handleException(exception, message, LoginActivity.class);
  }

  /**
   * Navigates the user from the current login screen to the registration screen.
   * <p>
   * This method is triggered when the user clicks the register redirection link.
   * It initializes and starts an intent to launch the {@link LoginActivity}.
   * </p>
   */
  private void handleRegisterLink() {
    startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
    finish();
  }

  /**
   * Initiates the asynchronous email verification delivery process.
   * <p>
   * This method activates the background progress bar indicator and offloads
   * processing to a single-thread background executor. On the background thread,
   * it makes an authentication API call to dispatch the verification link. Results
   * and encountered exceptions are subsequently piped back to the main UI thread to
   * update the text fields, reset the progress indicator, and display a confirmation toast.
   * </p>
   */
  private void handleVerifyEmailLink() {
    toggleProgressBarVisibility();
    Executors.newSingleThreadExecutor().execute(() -> {
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
        runOnUiThread(() -> handleException(exception, getString(R.string.error_general), LoginActivity.class));
      }
    });
  }

  private void handleResetPasswordLink(View v) {
    EditText resetMail = new EditText(v.getContext());
    AlertDialog.Builder passwordResetDialog = new AlertDialog.Builder(v.getContext());
    passwordResetDialog.setTitle(getString(R.string.password_reset_dialog_title));
    passwordResetDialog.setMessage(getString(R.string.password_reset_dialog_message));
    passwordResetDialog.setView(resetMail);
    passwordResetDialog.setPositiveButton(
            getString(R.string.password_reset_dialog_positive),
            (dialog, which) -> this.handleResetPassword(resetMail.getText().toString().trim())
    );
    passwordResetDialog.setNegativeButton(
            getString(R.string.password_reset_dialog_negative),
            (dialog, which) -> {
              // DO NOTHING
            }
    );
    passwordResetDialog.show();
  }
  private void handleResetPassword(String email) {
    Executors.newSingleThreadExecutor().execute(() -> {
      try {
        authService.sendPasswordResetEmail(email);
        runOnUiThread(() -> showToast(getString(R.string.password_reset_dialog_success)));
      } catch(AuthException exception) {
        runOnUiThread(() -> this.handleException(exception));
      } catch (Exception exception) {
        runOnUiThread(() -> this.handleException(exception, getString(R.string.error_general), LoginActivity.class));
      }
    });
  }

  /**
   * Validates the user input fields within the registration form.
   * This method extracts data from the email and password. It performs presence
   * and integrity constraints checking sequentially. If any field fails its validation rule,
   * the method immediately stops execution and routes the error message and failing view ID
   * via a custom exception.
   *
   * @throws ValidationException If any input field is empty.
   * */
  private void validateForm() throws ValidationException {
    String email = etEmail.getText().toString().trim();
    String password = etPassword.getText().toString();

    if(email.isEmpty()) throw new ValidationException(getString(R.string.email_required), R.id.etEmail);
    if(password.isEmpty()) throw new ValidationException(getString(R.string.password_required), R.id.etPassword);
  }
}