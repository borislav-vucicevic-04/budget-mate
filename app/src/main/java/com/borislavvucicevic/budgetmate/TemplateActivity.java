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
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;

import com.borislavvucicevic.budgetmate.exceptions.ValidationException;
import com.borislavvucicevic.budgetmate.services.AuthService;
import com.borislavvucicevic.budgetmate.services.DatabaseService;
import com.borislavvucicevic.budgetmate.services.LocalisationService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

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
  // ===========================================
  // FIELDS
  // ===========================================
  /**
   * Service used for authentication-related operations.
   */
  protected final AuthService authService = new AuthService();

  /**
   * Service used for database-related operations.
   */
  protected final DatabaseService databaseService = new DatabaseService();

  /**
   * A single-thread executor for executing processes in the background thread.
   * */
  private ExecutorService executor;

  /**
   * Spinner used to allow the user to select the application's locale.
   *
   * <p>Subclasses should assign this field inside {@link #grabWidgets()} before
   * the localization service is configured.</p>
   */
  private Spinner localeSwitch;

  /**
   * Overlay used to block UI while background process is running.
   *
   * <p>Subclasses should assign this field inside {@link #grabWidgets()} before
   * calling {@link #toggleBlocker()}.</p>
   * */
  private View blocker;

  /**
   * Optional progress bar used to indicate that an asynchronous or
   * long-running operation is in progress.
   *
   * <p>If a subclass uses a progress bar, it should assign this field inside
   * {@link #grabWidgets()}. If the activity does not require a progress bar,
   * this field may remain {@code null}.</p>
   */
  @Nullable
  private ProgressBar progressBar;

  /**
   * Optional text view used for displaying error messages to the user.
   *
   * <p>If assigned, the view is made visible and populated with an error
   * message when an exception is handled by
   * {@link #handleException(Exception, String, Class)}.</p>
   */
  @Nullable
  private TextView tvErrorWrapper;


  // ===========================================
  // SUBCLASS CONTRACT
  // ===========================================
  /**
   * Returns the layout resource used by this activity.
   *
   * @return the activity's layout resource identifier
   */
  @UiThread
  protected abstract int getLayoutID();



  // ===========================================
  // LIFECYCLES
  // ===========================================
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

  @Override
  protected void onDestroy() {
    super.onDestroy();
    this.killExecutor(true);
  }



  // ===========================================
  // INITIALIZATION HOOKS
  // ===========================================
  /**
   * Finds and assigns the UI widgets defined in the activity's layout.
   *
   * <p>This method is called after the layout has been set and before event
   * listeners are registered.</p>
   */
  @UiThread
  protected void grabWidgets() {
    tvErrorWrapper= findViewById(R.id.tvErrorWrapper);
    localeSwitch = findViewById(R.id.localeSwitch);
    progressBar = findViewById(R.id.progressBar);
    blocker = findViewById(R.id.blocker);
  }

  /**
   * Registers listeners for the activity's UI widgets.
   */
  @UiThread
  protected void setListeners() {
    localeSwitch.setOnItemSelectedListener(LocalisationService.getLocaleChangeHandler());
  }

  /**
   * Grabs values from the widgets and stores them in the activity fields
   * */
  @UiThread
  protected void grabValues() {
    // DO NOTHING
    // Because not all children should implement this method.
  }



  // ===========================================
  // UI HELPERS
  // ===========================================
  /**
   * Toggles the visibility of the activity's progress bar.
   *
   * <p>If the progress bar is currently visible, it is hidden using
   * {@link View#GONE}. Otherwise, it is made visible.</p>
   *
   * <p>If {@link #progressBar} is {@code null}, this method performs no action.</p>
   */
  @UiThread
  protected final void toggleProgressBar() {
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
   * Toggles the visibility of the activity's blocker.
   *
   * <p>If the blocker is currently visible, it is hidden using {@link View#GONE}</p>
   *
   * @throws IllegalStateException if you call this method without initialising {@link #blocker}
   * in {@link #grabWidgets()}
   * */
  @UiThread
  protected final void toggleBlocker() {
    if (blocker == null) {
      throw new IllegalStateException("You have not initialised 'blocker' widget. " +
              "Initialise it to use this method");
    }

    blocker.setVisibility(
            blocker.getVisibility() != View.VISIBLE ?
                    View.VISIBLE :
                    View.GONE
    );
  }

  /**
   * Displays a short-duration toast message to the user.
   *
   * @param message the message to display; must not be {@code null}
   */
  @UiThread
  protected final void showToast(@NonNull String message) {
    Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
    ).show();
  }



  // ===========================================
  // NAVIGATION
  // ===========================================
  /**
   * Opens the specified activity using the current activity as the context.
   *
   * <p>This overload uses the default behavior of keeping the current activity
   * active and preserving the existing activity task.</p>
   *
   * @param activityClass the activity class to open; must not be {@code null}
   */
  @UiThread
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
  @SuppressWarnings({"unused", "SameParameterValue"})
  @UiThread
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
  @UiThread
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



  // ===========================================
  // BACKGROUND EXECUTION
  // ===========================================
  /**
   * Executes the specified task asynchronously using the default exception-handling behavior.
   *
   * <p>This is a convenience overload of
   * {@link #doInBackground(Runnable, Runnable, Consumer, Class)} that does not provide
   * a custom exception handler or custom exception type.</p>
   *
   * <p>The {@code heavyTask} is executed on a background thread. If it completes
   * successfully, {@code whenDone} is executed through the standard completion
   * mechanism. Any exceptions thrown by the background task are handled by the
   * predefined exception handlers of the underlying implementation.</p>
   *
   * <p><strong>Important:</strong> this method must be called on the UI thread.</p>
   *
   * <p>If {@code heavyTask} is {@code null}, the call returns without performing
   * any action.</p>
   *
   * @param heavyTask the task to execute in the background; may be {@code null}
   * @param whenDone the action to perform after {@code heavyTask} completes successfully
   *
   * @see #doInBackground(Runnable, Runnable, Consumer, Class)
   */
  @UiThread
  protected final void doInBackground(
          Runnable heavyTask,
          Runnable whenDone
  ) {
    this.doInBackground(
            heavyTask,
            whenDone,
            null,
            null
    );
  }

  /**
   * Executes the given task asynchronously on a dedicated single-thread executor.
   *
   * <p>This method must be called from the UI thread. Before starting the background
   * task, it updates the UI to indicate that work is in progress. The supplied
   * {@code heavyTask} is then executed on a worker thread.</p>
   *
   * <p>If the task completes successfully and the worker thread has not been interrupted,
   * {@code whenDone} is passed to {@link #done(Runnable)} for execution after the
   * background work has finished.</p>
   *
   * <p>If the task throws an exception, exception handling is performed through
   * {@link #done(Runnable)} unless the worker thread has been interrupted. Exceptions
   * are handled according to the following rules:</p>
   *
   * <ul>
   *   <li>If the exception is an instance of {@code type} and
   *       {@code customExceptionHandler} is not {@code null}, the custom handler is invoked.</li>
   *   <li>If the exception is a {@link ValidationException}, it is handled by the
   *       predefined validation exception handler.</li>
   *   <li>All other exceptions are handled by the general exception handler.</li>
   * </ul>
   *
   * <p>If the worker thread is interrupted, for example because the associated Activity
   * is being destroyed, no further UI-related actions are performed.</p>
   *
   * <p>If {@code heavyTask} is {@code null}, this method returns immediately without
   * performing any action.</p>
   *
   * @param heavyTask the task to execute on the background thread; may be {@code null}
   * @param whenDone the action to perform after successful completion of
   *                 {@code heavyTask}; passed to {@link #done(Runnable)}
   * @param customExceptionHandler handler invoked when the thrown exception matches
   *                               {@code type}; may be {@code null}
   * @param type the exception type handled by {@code customExceptionHandler}
   */
  @UiThread
  @SuppressWarnings("java:S2095")
  protected final <T extends Exception> void doInBackground(
          Runnable heavyTask,
          Runnable whenDone,
          Consumer<T> customExceptionHandler,
          Class<T> type
  ) {
    if(heavyTask == null) {
      return;
    }
    if(this.executor != null) {
      return;
    }
    if ((type == null) != (customExceptionHandler == null)) {
      throw new IllegalArgumentException("Both 'type' and 'customExceptionHandler' must be provided together, or both must be null.");
    }

    this.toggleProgressBar();
    this.toggleBlocker();
    this.executor = Executors.newSingleThreadExecutor();

    this.executor.submit(() -> {
      try {
        this.sleep();
        heavyTask.run();
        this.done(whenDone);
      } catch (Exception exception) {
        this.handleBackgroundException(exception, customExceptionHandler, type);
      }
    });
  }

  // ===========================================
  // EXCEPTION HANDLERS
  // ===========================================
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
  @UiThread
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
   * to the user. <strong>Must be called and UI thread.</strong>
   *
   * <p>The exception is logged using the supplied class name as the log tag.
   * The provided display message is shown as a toast, and in the {@link #tvErrorWrapper} if
   * it exists.</p>
   *
   * @param exception the exception to log and handle; must not be {@code null}
   * @param displayMessage the user-friendly error message to display;
   *                       must not be {@code null}
   * @param type the class whose name should be used as the log tag;
   *             must not be {@code null}
   */
  @UiThread
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
  }



  // ===========================================
  // BACKGROUND EXECUTION INTERNALS
  // ===========================================
  /**
   * Puts the current thread to sleep for the configured amount of time. Mainly used to make sure,
   * progress bar is visible for at least 2 seconds.
   *
   * <p><strong>Important:</strong> This method must never be called from the main
   * (UI) thread. It blocks the calling thread and may cause the UI to freeze
   * and become unresponsive. This method should only be invoked from background
   * processes or worker threads where blocking is safe.</p>
   *
   * @throws RuntimeException if the current thread is interrupted while sleeping
   * @apiNote Sleeping time is two seconds.
   */
  @WorkerThread
  private void sleep() {
    int threadSleepingTime = 2000;
    try {
      Thread.sleep(threadSleepingTime);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("For some reason, sleep was interrupted", exception);
    }
  }

  /**
   * Handles an exception thrown during background execution.
   *
   * <p>Uses a matching custom handler when available, otherwise falls back to
   * validation or general exception handling.</p>
   *
   * @param exception the exception to handle
   * @param customExceptionHandler optional custom exception handler
   * @param type exception type handled by the custom handler
   * @param <T> custom exception type
   */
  @WorkerThread
  private <T extends Exception> void handleBackgroundException(
          Exception exception,
          Consumer<T> customExceptionHandler,
          Class<T> type
  ) {
    Runnable handler;
    if(type != null && type.isInstance(exception)) {
      handler = () -> customExceptionHandler.accept(type.cast(exception));
    } else if (exception instanceof ValidationException) {
      handler = () -> this.handleException((ValidationException) exception, this.getClass());
    } else {
      handler = () -> this.handleException(exception, getString(R.string.error_general), this.getClass());
    }

    this.done(handler);
  }

  /**
   * Toggles the visibility of the progress bar and blocker, and executes provided action on the
   * UI thread. Always called by the background process which is being executed by the
   * {@link #doInBackground(Runnable, Runnable, Consumer, Class) method.}
   *
   * <p><strong>Important:</strong> must be called on the worker thread.</p>
   * */
  @WorkerThread
  private void done(Runnable action) {
    runOnUiThread(() -> {
      if (isFinishing() || isDestroyed()) {
        this.killExecutor(false);
        return;
      }

      try {
        this.toggleProgressBar();
        this.toggleBlocker();

        if (action != null) {
          action.run();
        }
      } finally {
        this.killExecutor(false);
      }
    });
  }

  /**
   * Shuts down the executor and clears its reference.
   *
   * <p>If {@code force} is {@code true}, running tasks are interrupted immediately.
   * Otherwise, the executor is shut down gracefully.</p>
   *
   * @param force whether to force immediate shutdown
   */
  private void killExecutor(boolean force) {
    if(this.executor == null) {
      return;
    }

    if(force) {
      executor.shutdownNow();
    } else {
      executor.shutdown();
    }

    this.executor = null;
  }
}