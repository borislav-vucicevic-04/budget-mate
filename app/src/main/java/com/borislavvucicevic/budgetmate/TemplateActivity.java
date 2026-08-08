package com.borislavvucicevic.budgetmate;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.borislavvucicevic.budgetmate.exceptions.ValidationException;
import com.borislavvucicevic.budgetmate.services.AuthService;
import com.borislavvucicevic.budgetmate.services.DatabaseService;
import com.borislavvucicevic.budgetmate.services.LocalisationService;

/**
 * Base activity that defines common functionality and initialization logic
 * shared by the application's activities.
 *
 * <p>Subclasses must provide their layout resource, initialize their UI widgets,
 * and register their event listeners by implementing {@link #getLayoutID()},
 * {@link #grabWidgets()}, and {@link #setListeners()}.</p>
 *
 * <p>This activity also provides shared authentication and database services,
 * locale configuration, progress bar handling, toast messages, and common
 * exception handling.</p>
 */
public abstract class TemplateActivity extends AppCompatActivity {

  /**
   * Service used for authentication-related operations.
   */
  protected final AuthService authService = new AuthService();

  /**
   * Service used for database-related operations.
   */
  protected final DatabaseService databaseService = new DatabaseService();

  /**
   * Spinner used to allow the user to select the application's locale.
   *
   * <p>Subclasses should assign this field inside {@link #grabWidgets()} before
   * the localization service is configured.</p>
   */
  protected Spinner localeSwitch;

  /**
   * Optional progress bar used to indicate that an asynchronous or
   * long-running operation is in progress.
   *
   * <p>If a subclass uses a progress bar, it should assign this field inside
   * {@link #grabWidgets()}. If the activity does not require a progress bar,
   * this field may remain {@code null}.</p>
   */
  @Nullable
  protected ProgressBar progressBar;

  /**
   * Optional text view used for displaying error messages to the user.
   *
   * <p>If assigned, the view is made visible and populated with an error
   * message when an exception is handled by
   * {@link #handleException(Exception, String, Class)}.</p>
   */
  @Nullable
  protected TextView tvErrorWrapper;

  /**
   * Returns the layout resource used by this activity.
   *
   * @return the activity's layout resource identifier
   */
  protected abstract int getLayoutID();

  /**
   * Finds and assigns the UI widgets defined in the activity's layout.
   *
   * <p>This method is called after the layout has been set and before event
   * listeners are registered.</p>
   */
  protected abstract void grabWidgets();

  /**
   * Registers listeners for the activity's UI widgets.
   */
  protected abstract void setListeners();

  /**
   * Toggles the visibility of the activity's progress bar.
   *
   * <p>If the progress bar is currently visible, it is hidden using
   * {@link View#GONE}. Otherwise, it is made visible.</p>
   *
   * <p>If {@link #progressBar} is {@code null}, this method performs no action.</p>
   */
  protected void toggleProgressBarVisibility() {
    if (progressBar == null) {
      return;
    }

    progressBar.setVisibility(
            progressBar.getVisibility() == View.VISIBLE
                    ? View.GONE
                    : View.VISIBLE
    );
  }

  /**
   * Displays a short-duration toast message to the user.
   *
   * @param message the message to display; must not be {@code null}
   */
  protected void showToast(@NonNull String message) {
    Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
    ).show();
  }

  /**
   * Handles a validation exception and displays the corresponding validation
   * error to the user.
   *
   * <p>If the exception contains a view identifier, the associated
   * {@link EditText} receives the exception message as its validation error.</p>
   *
   * <p>The exception is then passed to the general exception handler. If the
   * exception does not contain a message, a generic error message is used.</p>
   *
   * @param exception the validation exception to handle; must not be {@code null}
   * @param type the class associated with the exception, used as the log tag;
   *             must not be {@code null}
   *
   * @see #handleException(Exception, String, Class)
   */
  protected void handleException(
          @NonNull ValidationException exception,
          @NonNull Class<?> type
  ) {
    if (exception.getViewID() != null) {
      ((EditText) findViewById(exception.getViewID()))
              .setError(exception.getMessage());
    }

    String message = exception.getMessage();

    this.handleException(
            exception,
            message != null
                    ? message
                    : getString(R.string.error_general),
            type
    );
  }

  /**
   * Handles an exception by logging it and displaying an error message
   * to the user.
   *
   * <p>The exception is logged using the supplied class name as the log tag.
   * The provided display message is shown as a toast.</p>
   *
   * <p>If {@link #tvErrorWrapper} is available, it is made visible and updated
   * with the error message. If {@link #progressBar} is available, it is hidden.</p>
   *
   * @param exception the exception to log and handle; must not be {@code null}
   * @param displayMessage the user-friendly error message to display;
   *                       must not be {@code null}
   * @param type the class whose name should be used as the log tag;
   *             must not be {@code null}
   */
  protected void handleException(
          @NonNull Exception exception,
          @NonNull String displayMessage,
          @NonNull Class<?> type
  ) {
    Log.e(type.getName(), exception.getMessage(), exception);

    showToast(displayMessage);

    if (tvErrorWrapper != null) {
      tvErrorWrapper.setVisibility(View.VISIBLE);
      tvErrorWrapper.setText(displayMessage);
    }

    if (progressBar != null) {
      progressBar.setVisibility(View.GONE);
    }
  }

  /**
   * Opens the specified activity using the current activity as the context.
   *
   * <p>This overload uses the default behavior of keeping the current activity
   * active and preserving the existing activity task.</p>
   *
   * @param activityClass the activity class to open; must not be {@code null}
   */
  protected final void openActivity(
          Class<? extends AppCompatActivity> activityClass
  ) {
    this.openActivity(
            activityClass,
            false,
            false,
            false
    );
  }

  /**
   * Opens the specified activity using either the application context or the
   * current activity as the context.
   *
   * <p>The current activity remains active and the existing activity task is
   * preserved.</p>
   *
   * @param activityClass the activity class to open; must not be {@code null}
   * @param shouldOpenInAppContext {@code true} to use the application context;
   *                               {@code false} to use the current activity
   */
  protected final void openActivity(
          Class<? extends AppCompatActivity> activityClass,
          boolean shouldOpenInAppContext
  ) {
    this.openActivity(
            activityClass,
            shouldOpenInAppContext,
            false,
            false
    );
  }

  /**
   * Opens the specified activity and optionally finishes the current activity.
   *
   * <p>The activity can be launched using either the application context or the
   * current activity. The existing activity task is preserved.</p>
   *
   * @param activityClass the activity class to open; must not be {@code null}
   * @param shouldOpenInAppContext {@code true} to use the application context;
   *                               {@code false} to use the current activity
   * @param shouldFinish {@code true} to finish the current activity after
   *                     starting the new activity; {@code false} otherwise
   */
  protected final void openActivity(
          Class<? extends AppCompatActivity> activityClass,
          boolean shouldOpenInAppContext,
          boolean shouldFinish
  ) {
    this.openActivity(
            activityClass,
            shouldOpenInAppContext,
            shouldFinish,
            false
    );
  }

  /**
   * Opens the specified activity with configurable context, activity finishing,
   * and task-clearing behavior.
   *
   * @param activityClass the activity class to open; must not be {@code null}
   * @param shouldOpenInAppContext {@code true} to use the application context;
   *                               {@code false} to use the current activity
   * @param shouldFinish {@code true} to finish the current activity after
   *                     starting the target activity; {@code false} otherwise
   * @param shouldClearActivityTask {@code true} to clear the existing activity
   *                                task and start the target activity in a new
   *                                task; {@code false} to preserve the task
   */
  protected final void openActivity(
          Class<? extends AppCompatActivity> activityClass,
          boolean shouldOpenInAppContext,
          boolean shouldFinish,
          boolean shouldClearActivityTask
  ) {
    Context context = shouldOpenInAppContext
            ? getApplicationContext()
            : this;

    Intent intent = new Intent(context, activityClass);

    if (shouldClearActivityTask) {
      intent.setFlags(
              Intent.FLAG_ACTIVITY_NEW_TASK |
                      Intent.FLAG_ACTIVITY_CLEAR_TASK
      );
    }

    startActivity(intent);

    if (shouldFinish) {
      this.finish();
    }
  }

  /**
   * Initializes the activity.
   *
   * <p>This method sets the activity layout, initializes its widgets, registers
   * its event listeners, and configures the locale-selection spinner.</p>
   *
   * @param savedInstanceState the previously saved activity state, or
   *                           {@code null} when the activity is created for
   *                           the first time
   */
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    this.setContentView(this.getLayoutID());
    this.grabWidgets();
    this.setListeners();

    LocalisationService.setLocaleSwitch(localeSwitch, this);
  }

  /**
   * Reconfigures the locale-selection spinner whenever the activity returns
   * to the foreground.
   */
  @Override
  protected void onResume() {
    super.onResume();

    LocalisationService.setLocaleSwitch(localeSwitch, this);
  }
}