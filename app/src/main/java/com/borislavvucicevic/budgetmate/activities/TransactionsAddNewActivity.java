package com.borislavvucicevic.budgetmate.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.models.classes.Category;
import com.borislavvucicevic.budgetmate.models.classes.Transaction;
import com.borislavvucicevic.budgetmate.models.exceptions.ValidationException;
import com.borislavvucicevic.budgetmate.services.AuthService;
import com.borislavvucicevic.budgetmate.services.CacheService;
import com.borislavvucicevic.budgetmate.services.DatabaseService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TransactionsAddNewActivity extends AppCompatActivity {
  public static final String TRANSACTION_ADD_NEW_ACTIVITY = "TRANSACTION_ADD_NEW_ACTIVITY";
  private AuthService authService;
  private DatabaseService databaseService;
  private AutoCompleteTextView etCategory;
  private TextView tvErrorWrapper;
  private EditText etAmount;
  private EditText etNotes;
  private ProgressBar progressBar;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_transactions_add_new);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
      int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
      v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), imeBottom);
      return insets;
    });

    // setting up services
    authService = new AuthService();
    databaseService = new DatabaseService();

    // grabbing widgets
    Button btnCreateTransaction = findViewById(R.id.btnCreateTransaction);
    etCategory = findViewById(R.id.etCategory);
    tvErrorWrapper = findViewById(R.id.tvErrorWrapper);
    etAmount = findViewById(R.id.etAmount);
    etNotes = findViewById(R.id.etNotes);
    progressBar = findViewById(R.id.progressBar);
    // setting listeners
    btnCreateTransaction.setOnClickListener(v -> this.handleCreateTransaction());
    etCategory.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus) {
        etCategory.showDropDown();
      }
    });
    etCategory.setOnClickListener(v -> etCategory.showDropDown());

    // setting the autocomplete textview
    this.setEtCategory();

    // setting up the back press interceptor
    getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
      @Override
      public void handleOnBackPressed() {
        // Trigger the confirmation layout when the user attempts to exit
        showDiscardChangesDialog();
      }
    });
  }

  /**
   * Asynchronously fetches the user's categories from the database and populates
   * the {@code etCategory} dropdown adapter on the UI thread.
   * <p>
   * This method runs the network/database operations on a background single-thread
   * executor to avoid blocking the main UI thread. Once the category names are
   * retrieved and mapped, it switches back to the UI thread using {@code runOnUiThread}
   * to safely update the AutoCompleteTextView or Spinner adapter.
   * </p>
   * <p>
   * Any exceptions caught during the background execution or UI update are routed
   * back to the main thread via {@link #handleException(Exception)}.
   * </p>
   */
  private void setEtCategory() {
    Executors.newSingleThreadExecutor().execute(() -> {
      try {
        String uid = authService.getUserID();
        List<Category> categories = databaseService.getCategories(uid);
        List<String> categoryNames = categories.stream().map(Category::getName).collect(Collectors.toList());
        runOnUiThread(() -> {
          ArrayAdapter<String> adapter = new ArrayAdapter<>(
                  this,
                  android.R.layout.simple_dropdown_item_1line,
                  categoryNames
          );
          etCategory.setAdapter(adapter);
          Toast.makeText(
                  TransactionsAddNewActivity.this,
                  getString(R.string.transactions_categories_loaded),
                  Toast.LENGTH_SHORT
          ).show();
        });
      } catch (Exception exception) {
        runOnUiThread(() -> this.handleException(exception));
      }
    });
  }

  /**
   * Handles the asynchronous creation and insertion of a new financial transaction.
   * <p>
   * This method extracts the transaction data (amount, notes, and category name) from the UI components
   * and displays a loading indicator. It then spawns a background thread via an executor to perform
   * input validation and database insertion to prevent blocking the main UI thread.
   * </p>
   *
   * @see CacheService
   * @see DatabaseService
   * @see Transaction
   * @see Category
   */
  private void handleCreateTransaction() {
    String amount = etAmount.getText().toString().trim();
    String notes = etNotes.getText().toString().trim();
    String categoryName = etCategory.getText().toString().trim();
    progressBar.setVisibility(View.VISIBLE);
    Executors.newSingleThreadExecutor().execute(() -> {
      try {
        this.handleValidation();
        // targeting category
        List<Category> categories = CacheService.readCategories();
        Category category = categories
                .stream().
                filter(item -> categoryName.equals(item.getName()))
                .findFirst()
                .orElse(new Category(null, categoryName, authService.getUserID()));

        // creating transaction
        Transaction transaction = new Transaction(
                authService.getUserID(),
                Float.parseFloat(amount),
                category,
                Timestamp.now(),
                !notes.isEmpty() ? notes : null
        );
        databaseService.insertTransaction(transaction);
        runOnUiThread(this::handleSuccess);
      } catch (ValidationException exception) {
        runOnUiThread(() -> this.handleException(exception));
      } catch (Exception exception) {
        runOnUiThread(() -> this.handleException(exception));
      }
    });
  }

  /**
   * Finalizes the transaction creation process on the UI thread after a successful database insertion.
   * <p>
   * This method provides visual feedback to the user, stops active loading animations,
   * and navigates the user back to the previous screen by popping this Activity off the application stack.
   * </p>
   *
   * <p><b>UI Actions Performed:</b></p>
   * <ul>
   *   <li>Displays a short-duration {@link Toast} message notifying the user that the transaction was successfully saved.</li>
   *   <li>Hides the active {@code progressBar} by setting its visibility to {@link android.view.View#GONE}.</li>
   *   <li>Invokes {@link #finish()} to close the current {@code TransactionsAddNewActivity}.</li>
   * </ul>
   *
   * @see android.widget.Toast
   * @see android.view.View#GONE
   * @see #finish()
   */
  private void handleSuccess() {
    Toast.makeText(
            TransactionsAddNewActivity.this,
            getString(R.string.transaction_success),
            Toast.LENGTH_SHORT
    ).show();
    progressBar.setVisibility(View.GONE);
    finish();
  }

  /**
   * Validates the transaction input fields against business logic rules before data submission.
   * <p>
   * This method extracts text from the UI components, trims whitespace, and evaluates the values sequentially.
   * If any business rule is violated, it immediately stops execution and throws a custom exception
   * bundled with a localized error message and the target resource ID of the failing input field.
   * </p>
   *
   * <p><b>Validation Rules Applied:</b></p>
   * <ul>
   *   <li><b>Amount Required:</b> Evaluates if the amount field is empty.</li>
   *   <li><b>Non-Zero Value:</b> Parses the amount to check if the numerical value equals exactly zero.</li>
   *   <li><b>Category Required:</b> Evaluates if a budget or expense category name has been provided.</li>
   *   <li><b>Notes Length Boundary:</b> Ensures that optional descriptive notes do not exceed a hard limit of 256 characters.</li>
   * </ul>
   *
   * @throws ValidationException if any input field contains invalid, missing, or out-of-bounds data.
   *
   * @see ValidationException
   */
  private void handleValidation() {
    String amount = etAmount.getText().toString().trim();
    String category = etCategory.getText().toString().trim();
    String notes = etNotes.getText().toString().trim();

    if(amount.isEmpty()) throw new ValidationException(getString(R.string.amount_required), R.id.etAmount);
    if(Float.parseFloat(amount) == 0) throw new ValidationException(getString(R.string.amount_not_zero), R.id.etAmount);
    if(category.isEmpty()) throw new ValidationException(getString(R.string.category_required), R.id.etCategory);
    if(notes.length() > 256) throw new ValidationException(getString(R.string.notes_too_long), R.id.etNotes);
  }

  /**
   * Processes targeted business logic validation errors to update the user interface and provide feedback.
   * <p>
   * This handler maps the details inside a {@link ValidationException} back to the specific input field
   * that failed validation. It highlights the target component, logs the error stack trace, reveals
   * a localized error label, pops up a short Toast notification, and hides the loading spinner.
   * </p>
   *
   * @param exception the structural data payload tracking the invalid field ID and localized error text.
   *
   * @see ValidationException
   */
  private void handleException(ValidationException exception) {
    if(exception.getViewID() != null) {
      ((EditText) findViewById(exception.getViewID())).setError(exception.getMessage());
    }
    Log.e(TRANSACTION_ADD_NEW_ACTIVITY, exception.getMessage(), exception);
    tvErrorWrapper.setVisibility(TextView.VISIBLE);
    tvErrorWrapper.setText(exception.getMessage());
    Toast.makeText(
            TransactionsAddNewActivity.this,
            exception.getMessage(),
            Toast.LENGTH_SHORT
    ).show();
    progressBar.setVisibility(View.GONE);
  }

  /**
   * Handles exceptions by logging the error stack trace and displaying a generic
   * error message to the user via both a Toast notification and a UI text wrapper.
   * <p>
   * This method ensures that failures (such as database or network errors during
   * asynchronous operations) are gracefully reported visually to the user while
   * capturing full diagnostic details in the system logs.
   * </p>
   * <p>
   * <strong>Note:</strong> This method must be called on the UI thread because it
   * directly modifies the visibility/text of {@code tvErrorWrapper} and displays a Toast.
   * </p>
   *
   * @param exception The {@link Exception} caught during the operation, used to log
   *                  the error message and stack trace.
   */
  private void handleException(Exception exception) {
    Toast.makeText(
            TransactionsAddNewActivity.this,
            getString(R.string.error_general),
            Toast.LENGTH_SHORT
    ).show();
    tvErrorWrapper.setText(getString(R.string.error_general));
    Log.e(TRANSACTION_ADD_NEW_ACTIVITY, exception.getMessage(), exception);
  }

  private void showDiscardChangesDialog() {
    new MaterialAlertDialogBuilder(this, R.style.CustomAlertDialogTheme)
            .setTitle(R.string.discard_changes_title)
            .setMessage(R.string.discard_changes_message)
            .setPositiveButton(R.string.discard_changes_yes, (d, which) -> finish())
            .setNegativeButton(R.string.discard_changes_no, (d, which) -> d.dismiss())
            .setCancelable(true)
            .show();
  }
}