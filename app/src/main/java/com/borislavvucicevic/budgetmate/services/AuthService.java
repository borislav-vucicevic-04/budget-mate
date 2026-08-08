package com.borislavvucicevic.budgetmate.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.borislavvucicevic.budgetmate.exceptions.AuthException;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import java.util.concurrent.ExecutionException;

/**
 * Provides authentication services by wrapping the Firebase Authentication SDK.
 *
 * <p>This service handles user account operations synchronously by blocking the calling thread
 * until the underlying Firebase tasks complete. It maps Firebase-specific exceptions into
 * the application's custom {@link AuthException} architecture.</p>
 */
public class AuthService {

  /**
   * Tag used for system logging statements originating from this service.
   */
  private static final String AUTH_SERVICE = "AUTH_SERVICE";

  /**
   * The underlying Firebase Authentication instance used to execute requests.
   */
  private final FirebaseAuth firebaseAuth;

  /**
   * Constructs a new authentication service and initializes the Firebase Authentication instance.
   */
  public AuthService() {
    this.firebaseAuth = FirebaseAuth.getInstance();
  }

  /**
   * Checks if user is signed in.
   *
   * @return {@code true} if the user is logged in, otherwise {@code false}
   * */
  public boolean isSignedIn() {
    return firebaseAuth.getCurrentUser() != null;
  }

  /**
   * Returns the unique identifier of the currently authenticated Firebase user.
   *
   * <p>The method retrieves the currently signed-in {@link FirebaseUser} from
   * the Firebase authentication service. If no user is currently authenticated,
   * the method returns {@code null}.</p>
   *
   * @return the unique Firebase user ID, or {@code null} if no user is currently signed in
   */
  public String getUserID() {
    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
    if (firebaseUser == null) return null;
    return firebaseUser.getUid();
  }

  /**
   * Creates a new user account with the specified email address and password.
   *
   * <p>This method blocks until the account creation process finishes on the background network thread.</p>
   *
   * @param email    the unique email address for the new user account
   * @param password the secure password for the new user account
   * @return the unique Firebase UserProfile ID (UID) assigned to the newly created account
   * @throws AuthException if account creation fails due to Firebase errors (e.g., email already in use),
   *                       thread interruption, or if the server returns an empty user profile
   */
  public String createUserAccount(String email, String password) throws AuthException {
    try {
      AuthResult result = Tasks.await(firebaseAuth.createUserWithEmailAndPassword(email, password));
      FirebaseUser firebaseUser = result.getUser();

      if(firebaseUser != null) return firebaseUser.getUid();
      else throw new AuthException("UNKNOWN_ERROR", "Account creation completed, but user data is null.");
    }catch (ExecutionException e) {
      Throwable cause = e.getCause();

      // Check if the underlying failure was thrown by the Firebase SDK
      if (cause instanceof FirebaseAuthException) {
        FirebaseAuthException firebaseEx = (FirebaseAuthException) cause;
        String firebaseErrorCode = firebaseEx.getErrorCode(); // Returns "ERROR_EMAIL_ALREADY_IN_USE", etc.

        // Map the Firebase Error Code to your custom localized Exception architecture
        throw new AuthException(firebaseErrorCode, firebaseEx.getMessage(), firebaseEx);
      }

      // Handle fallback execution failures
      throw new AuthException("EXECUTION_ERROR", "Execution failed on background thread.", e);

    } catch (InterruptedException e) {
      Log.e(AUTH_SERVICE, e.getMessage(), e);
      Thread.currentThread().interrupt(); // Restore interrupted status
      throw new AuthException("INTERRUPTED_ERROR", "Authentication process was interrupted.", e);
    }
  }

  /**
   * Deletes the currently authenticated Firebase user account.
   *
   * <p>The user is first reauthenticated using the supplied email address and
   * password because deleting a Firebase Authentication account is a
   * security-sensitive operation that requires recent authentication.</p>
   *
   * <p>This method blocks until both the reauthentication and account deletion
   * operations have completed.</p>
   *
   * @param email    the email address of the currently authenticated user
   * @param password the user's current password
   * @throws AuthException if no user is signed in, reauthentication fails,
   *                       account deletion fails, or the operation is interrupted
   */
  private void deleteUserAccount(@NonNull String email, @NonNull String password) {
    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

    if (firebaseUser == null) {
      throw new AuthException(
              "ERROR_NO_SIGNED_IN_USER",
              "No user is currently signed in."
      );
    }

    if (email.trim().isEmpty()) {
      throw new AuthException(
              "ERROR_INVALID_EMAIL",
              "Email cannot be null or empty."
      );
    }

    if (password.isEmpty()) {
      throw new AuthException(
              "ERROR_INVALID_PASSWORD",
              "Password cannot be null or empty."
      );
    }

    try {
      /*
       * Create fresh credentials from the user's email and password.
       * These credentials are used to prove that the user requesting
       * account deletion is actually the account owner.
       */
      AuthCredential credential = EmailAuthProvider.getCredential(email, password);

      /*
       * Deleting an account is a security-sensitive operation.
       * Reauthenticate the user first so that Firebase considers
       * the authentication session recent.
       */
      Tasks.await(firebaseUser.reauthenticate(credential));

      /*
       * Reauthentication succeeded, so the Firebase Authentication
       * account can now be deleted.
       */
      Tasks.await(firebaseUser.delete());

      Log.d(
              AUTH_SERVICE,
              "Firebase user profile successfully deleted."
      );

    } catch (ExecutionException exception) {
      Throwable cause = exception.getCause();

      /*
       * Map Firebase Authentication exceptions to the application's
       * custom AuthException architecture.
       */
      if (cause instanceof FirebaseAuthException) {
        FirebaseAuthException firebaseEx = (FirebaseAuthException) cause;

        String firebaseErrorCode = firebaseEx.getErrorCode();

        throw new AuthException(
                firebaseErrorCode,
                firebaseEx.getMessage(),
                firebaseEx
        );
      }

      /*
       * Handle unexpected failures originating from the asynchronous
       * Firebase task execution.
       */
      throw new AuthException(
              "EXECUTION_ERROR",
              "Execution failed while deleting the user profile.",
              exception
      );

    } catch (InterruptedException exception) {
      Log.e(
              AUTH_SERVICE,
              "User profile deletion was interrupted.",
              exception
      );

      Thread.currentThread().interrupt();

      throw new AuthException(
              "INTERRUPTED_ERROR",
              "User profile deletion process was interrupted.",
              exception
      );
    }
  }

  /**
   * Sends a verification email to the currently authenticated user.
   *
   * <p>This method blocks until the email transmission process finishes on the background network thread.
   * It expects a user session to already exist; if no user is signed in, it immediately throws an
   * authentication exception.</p>
   *
   * @throws AuthException if no user is currently logged in, if email transmission fails due to
   *                       Firebase errors (e.g., too many requests, expired session token), or if
   *                       the thread is interrupted
   */
  public void sendVerificationEmail() throws AuthException {
    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

    if (firebaseUser == null) {
      throw new AuthException("ERROR_NO_SIGNED_IN_USER", "No user is currently signed in to receive a verification email.");
    }

    try {
      Tasks.await(firebaseUser.sendEmailVerification());
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();

      if (cause instanceof com.google.firebase.FirebaseTooManyRequestsException) {
        throw new AuthException(
                "ERROR_TOO_MANY_REQUESTS",
                "Email verification requests blocked due to unusual activity. Please try again later.",
                cause
        );
      }

      if (cause instanceof FirebaseAuthException) {
        FirebaseAuthException firebaseEx = (FirebaseAuthException) cause;
        String firebaseErrorCode = firebaseEx.getErrorCode();
        throw new AuthException(firebaseErrorCode, firebaseEx.getMessage(), firebaseEx);
      }

      throw new AuthException("EXECUTION_ERROR", "Execution failed while sending verification email.", e);
    } catch (InterruptedException e) {
      Log.e(AUTH_SERVICE, e.getMessage(), e);
      Thread.currentThread().interrupt();
      throw new AuthException("INTERRUPTED_ERROR", "Verification email process was interrupted.", e);
    }
  }

  /**
   * Sends a password reset email to the specified email address.
   *
   * <p>This method blocks until the email transmission process finishes on the background network thread.</p>
   *
   * @param email the recipient email address to send the password reset link to
   * @throws AuthException if email transmission fails due to Firebase errors (e.g., invalid email,
   *                       user not found, too many requests), or if the thread is interrupted
   */
  public void sendPasswordResetEmail(String email) throws AuthException {
    try {
      Tasks.await(firebaseAuth.sendPasswordResetEmail(email));
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();

      if (cause instanceof com.google.firebase.FirebaseTooManyRequestsException) {
        throw new AuthException(
                "ERROR_TOO_MANY_REQUESTS",
                "Password reset requests blocked due to unusual activity. Please try again later.",
                cause
        );
      }

      if (cause instanceof FirebaseAuthException) {
        FirebaseAuthException firebaseEx = (FirebaseAuthException) cause;
        String firebaseErrorCode = firebaseEx.getErrorCode();
        throw new AuthException(firebaseErrorCode, firebaseEx.getMessage(), firebaseEx);
      }

      throw new AuthException("EXECUTION_ERROR", "Execution failed while sending password reset email.", e);
    } catch (InterruptedException e) {
      Log.e(AUTH_SERVICE, e.getMessage(), e);
      Thread.currentThread().interrupt();
      throw new AuthException("INTERRUPTED_ERROR", "Password reset email process was interrupted.", e);
    }
  }

  /**
   * Signs in a user with the specified email address and password.
   *
   * <p>This method blocks until the authentication process finishes on the background network thread.</p>
   *
   * @param email    the registered email address of the user
   * @param password the password for the account
   * @return the unique Firebase UserProfile ID (UID) assigned to the authenticated account
   * @throws AuthException if sign-in fails due to Firebase errors (e.g., wrong password),
   *                       thread interruption, if the server returns an empty profile,
   *                       or if the user's email address is not verified
   */
  public String signIn(String email, String password) throws AuthException {
    try {
      AuthResult result = Tasks.await(firebaseAuth.signInWithEmailAndPassword(email, password));
      FirebaseUser firebaseUser = result.getUser();

      if (firebaseUser == null) {
        throw new AuthException("UNKNOWN_ERROR", "Sign-in completed, but user data is null.");
      }

      // Enforce email verification check
      if (!firebaseUser.isEmailVerified()) {
        throw new AuthException("ERROR_EMAIL_NOT_VERIFIED", "The email address for this account has not been verified.");
      }

      return firebaseUser.getUid();
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();

      // Check if the underlying failure was thrown by the Firebase SDK
      if (cause instanceof FirebaseAuthException) {
        FirebaseAuthException firebaseEx = (FirebaseAuthException) cause;
        String firebaseErrorCode = firebaseEx.getErrorCode(); // Returns "ERROR_WRONG_PASSWORD", etc.

        // Map the Firebase Error Code to your custom localized Exception architecture
        throw new AuthException(firebaseErrorCode, firebaseEx.getMessage(), firebaseEx);
      }

      // Handle fallback execution failures
      throw new AuthException("EXECUTION_ERROR", "Execution failed on background thread.", e);

    } catch (InterruptedException e) {
      Log.e(AUTH_SERVICE, e.getMessage(), e);
      Thread.currentThread().interrupt(); // Restore interrupted status
      throw new AuthException("INTERRUPTED_ERROR", "Authentication process was interrupted.", e);
    }
  }

  /**
   * Signs out the currently authenticated user from Firebase.
   *
   * <p>This method ends the current Firebase Authentication session by calling
   * {@link FirebaseAuth#signOut()}.</p>
   */
  public void signOut() {
    firebaseAuth.signOut();
  }
}