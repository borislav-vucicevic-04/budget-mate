package com.borislavvucicevic.budgetmate.activities;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.borislavvucicevic.budgetmate.MainActivity;
import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.TemplateActivity;
import com.borislavvucicevic.budgetmate.exceptions.AuthException;
import com.borislavvucicevic.budgetmate.enums.FirebaseAuthErrorCodes;
import com.borislavvucicevic.budgetmate.exceptions.ValidationException;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.function.Consumer;

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

  /**
   * Holds the value of {@link LoginActivity#etEmail} widget
   * */
  private String email;

  /**
   * Holds the value of {@link LoginActivity#etPassword} widget
   * */
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

    if(authService.isSignedIn()) {
      this.openActivity(
              MainActivity.class,
              true,
              true
      );
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
    super.grabWidgets();
    etEmail = findViewById(R.id.etEmail);
    etPassword = findViewById(R.id.etPassword);
    tvVerifyEmailLink = findViewById(R.id.tvVerifyEmailLink);
    btnLogin = findViewById(R.id.btnLogin);
    tvPasswordResetLink = findViewById(R.id.tvPasswordResetLink);
    tvRegisterLink = findViewById(R.id.tvRegisterLink);
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
    super.setListeners();
    btnLogin.setOnClickListener(v -> this.btnLoginClickHandler());
    tvRegisterLink.setOnClickListener(v -> this.openActivity(
            RegisterActivity.class,
            true,
            true
    ));
    tvVerifyEmailLink.setOnClickListener(v -> this.handleVerifyEmailLink());
    tvPasswordResetLink.setOnClickListener(this::handleResetPasswordLink);
  }

  /**
   * Grabs values of widgets and stores them in the class fields.
   * */
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
    this.doInBackground(
            this::handleLogin,
            this::handleSuccess,
            this::handleAuthException,
            AuthException.class
    );
  }

  /**
   * Validates the login form and attempts to authenticate the user.
   *
   * <p>This method performs the potentially blocking login operation and is intended
   * to be executed as a background task by using {@code doInBackground}.</p>
   *
   * <p>Any exception thrown by validation or authentication is propagated to
   * {@code doInBackground}, which handles it on the UI thread.</p>
   *
   * @see #doInBackground(Runnable, Runnable, Consumer, Class)
   */
  @WorkerThread
  private void handleLogin() {
    this.handleValidation();
    authService.signIn(email, password);
  }

  /**
   * Handles a successful login on the UI thread, by redirecting user to the dashboard.
   *
   * <p>This method is passed to {@code doInBackground} as the {@code whenDone}
   * action and is executed after the background login process completes successfully.</p>
   * 
   * @see #doInBackground(Runnable, Runnable, Consumer, Class)
   */
  @UiThread
  private void handleSuccess() {
    this.showToast(getString(R.string.login_success));
    this.openActivity(MainActivity.class, true, true);
    finish();
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

    // Log and display the message.
    this.handleException(exception, message, LoginActivity.class);
  }

  /**
   * Sends a verification email asynchronously.
   *
   * <p>On success, updates the verification link and shows a confirmation message.
   * Authentication errors are handled by {@link #handleAuthException(AuthException)}.</p>
   *
   * @see #doInBackground(Runnable, Runnable, Consumer, Class)
   * @see #handleAuthException(AuthException)
   */
  private void handleVerifyEmailLink() {
    this.doInBackground(
            // heavy task
            authService::sendVerificationEmail,
            // handler to be executed when task is done
            () -> {
              tvVerifyEmailLink.setText(getText(R.string.resend_verification_email));
              showToast(getString(R.string.verification_email_sent));
            },
            // handler for custom exceptions
            this::handleAuthException,
            // type of possible custom exceptions
            AuthException.class
    );
  }

  /**
   * Displays a dialog that allows the user to request a password reset.
   *
   * @param v view that triggered the password reset action; its context
   *          is used when creating the dialog
   */
  private void handleResetPasswordLink(@NonNull View v) {

    EditText etPassword = new EditText(v.getContext());

    int margin = getResources().getDimensionPixelSize(R.dimen.dialog_input_margin);
    int minHeight = getResources().getDimensionPixelSize(R.dimen.dialog_input_min_height);

    // Background
    etPassword.setBackgroundResource(R.drawable.bg_input_field);
    // Text color
    etPassword.setTextColor(ContextCompat.getColor(v.getContext(), R.color.black));
    // Hint text color
    etPassword.setHintTextColor(ContextCompat.getColor(v.getContext(), R.color.gray));
    // Minimum height: 48dp
    etPassword.setMinHeight(minHeight);
    // Password input
    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
    etPassword.setHint(getString(R.string.password));
    // Container gives the EditText a 8dp margin around it
    FrameLayout container = new FrameLayout(v.getContext());
    // Layout parameters
    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
    );

    // setting container padding and layout parameters
    container.setPadding(margin, margin, margin, margin);
    container.addView(etPassword, params);

    // creating the dialog
    MaterialAlertDialogBuilder dialog =
            new MaterialAlertDialogBuilder(this, R.style.CustomAlertDialogTheme);

    // setting dialog parameters
    dialog.setTitle(getString(R.string.password_reset_dialog_title));
    dialog.setMessage(getString(R.string.password_reset_dialog_message));
    dialog.setView(container);
    dialog.setPositiveButton(
            getString(R.string.password_reset_dialog_positive),
            (d, which) -> {
              String password = etPassword.getText().toString().trim();
              this.handleResetPassword(password);
            }
    );
    dialog.setNegativeButton(
            getString(R.string.password_reset_dialog_negative),
            (d, which) -> d.dismiss()
    );

    dialog.show();
  }

  /**
   * Sends a password reset email asynchronously.
   *
   * <p>On success, displays a confirmation message. Authentication errors are
   * handled by {@link #handleAuthException(AuthException)}.</p>
   *
   * @param email the email address to send the reset link to
   * @see #doInBackground(Runnable, Runnable, Consumer, Class) 
   * @see #handleAuthException(AuthException) 
   */
  private void handleResetPassword(String email) {
    this.doInBackground(
            () -> authService.sendPasswordResetEmail(email),
            () -> showToast(getString(R.string.password_reset_dialog_success)),
            this::handleAuthException,
            AuthException.class
    );
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
  private void handleValidation() throws ValidationException {
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