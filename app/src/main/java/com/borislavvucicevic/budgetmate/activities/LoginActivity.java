package com.borislavvucicevic.budgetmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.models.AuthException;
import com.borislavvucicevic.budgetmate.models.FirebaseAuthErrorCodes;
import com.borislavvucicevic.budgetmate.models.ValidationException;

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
  public static final String LOGIN_ACTIVITY = "LOGIN_ACTIVITY";
  private EditText etEmail, etPassword;
  private TextView tvErrorWrapper;
  private ProgressBar progressBar;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_login);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activityLogin), (v, insets) -> {
      int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
      v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), imeBottom);
      return insets;
    });

    // getting widgets
    etEmail = findViewById(R.id.etEmail);
    etPassword = findViewById(R.id.etPassword);
    tvErrorWrapper = findViewById(R.id.tvErrorWrapper);
    progressBar = findViewById(R.id.progressBar);
    Button btnLogin = findViewById(R.id.btnLogin);
    TextView tvRegisterLink = findViewById(R.id.tvRegisterLink);

    // setting event handlers
    btnLogin.setOnClickListener(v -> this.handleLogin());
    tvRegisterLink.setOnClickListener(v -> this.handleRegisterLink());
  }

  private void handleLogin() {
    String email = etEmail.getText().toString().trim();
    String password = etPassword.getText().toString();
    progressBar.setVisibility(View.VISIBLE);

    Executors.newSingleThreadExecutor().execute(() -> {
      try {
        this.validateForm();
      } catch (ValidationException exception)  {
        runOnUiThread(() -> this.handleException(exception));
      } catch(Exception exception) {
        runOnUiThread(() -> this.handleException(exception));
      }
    });
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
    Log.e(LOGIN_ACTIVITY, exception.getMessage(), exception);
    tvErrorWrapper.setVisibility(TextView.VISIBLE);
    tvErrorWrapper.setText(exception.getMessage());
    Toast.makeText(
            LoginActivity.this,
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
      case ERROR_USER_NOT_FOUND: message = getString(R.string.error_user_not_found); break;
      case ERROR_WRONG_PASSWORD: message = getString(R.string.error_wrong_password); break;
      case ERROR_EMAIL_NOT_VERIFIED: message = getString(R.string.error_email_not_verified); break;
      default: message = getString(R.string.error_general);
    }
    // logging and displaying the message
    Log.e(LOGIN_ACTIVITY, exception.getMessage(), exception);
    tvErrorWrapper.setVisibility(TextView.VISIBLE);
    tvErrorWrapper.setText(message);
    Toast.makeText(
            LoginActivity.this,
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
    Log.e(LOGIN_ACTIVITY, exception.getMessage(), exception);
    tvErrorWrapper.setVisibility(TextView.VISIBLE);
    tvErrorWrapper.setText(exception.getMessage());
    Toast.makeText(
            LoginActivity.this,
            getString(R.string.error_general),
            Toast.LENGTH_SHORT
    ).show();
    progressBar.setVisibility(View.INVISIBLE);
  }

  /**
   * Navigates the user from the current login screen to the registration screen.
   * <p>
   * This method is triggered when the user clicks the register redirection link.
   * It initializes and starts an intent to launch the {@link LoginActivity}.
   * </p>
   */
  private void handleRegisterLink() {
    startActivity(new Intent(getApplicationContext(), LoginActivity.class));
    finish();
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