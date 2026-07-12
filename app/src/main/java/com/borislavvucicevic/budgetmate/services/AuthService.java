package com.borislavvucicevic.budgetmate.services;

import android.util.Log;

import com.borislavvucicevic.budgetmate.models.AuthException;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
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
   * Creates a new user account with the specified email address and password.
   *
   * <p>This method blocks until the account creation process finishes on the background network thread.</p>
   *
   * @param email    the unique email address for the new user account
   * @param password the secure password for the new user account
   * @return the unique Firebase User ID (UID) assigned to the newly created account
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
}