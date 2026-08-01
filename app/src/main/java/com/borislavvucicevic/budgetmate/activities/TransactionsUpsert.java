package com.borislavvucicevic.budgetmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.models.Category;
import com.borislavvucicevic.budgetmate.models.Transaction;
import com.borislavvucicevic.budgetmate.options.TransactionTypeOption;
import com.borislavvucicevic.budgetmate.enums.CacheKey;
import com.borislavvucicevic.budgetmate.enums.TransactionType;
import com.borislavvucicevic.budgetmate.exceptions.ValidationException;
import com.borislavvucicevic.budgetmate.services.AuthService;
import com.borislavvucicevic.budgetmate.services.CacheService;
import com.borislavvucicevic.budgetmate.services.DatabaseService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TransactionsUpsert extends AppCompatActivity {
  public static final String TRANSACTION_ADD_NEW_ACTIVITY = "TRANSACTION_ADD_NEW_ACTIVITY";
  private AuthService authService;
  private DatabaseService databaseService;
  private AutoCompleteTextView etCategory;
  private TextView tvErrorWrapper;
  private EditText etAmount;
  private EditText etNotes;
  private Spinner spTransactionType;
  private ProgressBar progressBar;
  private Button btnUpsertTransaction;
  private Transaction transactionUpsertObject = CacheService.read(CacheKey.TRANSACTION_UPSERT_OBJECT, Transaction.class);
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_transactions_upsert);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
      int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
      v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), imeBottom);
      return insets;
    });

    // setting up services
    authService = new AuthService();
    databaseService = new DatabaseService();

    // grabbing widgets
    btnUpsertTransaction = findViewById(R.id.btnUpsertTransaction);
    etCategory = findViewById(R.id.etCategory);
    tvErrorWrapper = findViewById(R.id.tvErrorWrapper);
    etAmount = findViewById(R.id.etAmount);
    etNotes = findViewById(R.id.etNotes);
    spTransactionType = findViewById(R.id.spTransactionType);
    progressBar = findViewById(R.id.progressBar);
    // setting listeners
    btnUpsertTransaction.setOnClickListener(v -> this.handleUpsertTransaction());
    etCategory.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus) {
        etCategory.showDropDown();
      }
    });
    etCategory.setOnClickListener(v -> etCategory.showDropDown());

    // setting the autocomplete textview
    this.setEtCategory();

    // setting the spinner
    this.setSpTransactionType();

    // setting form values
    if(transactionUpsertObject != null) {
      this.setFormValues();
    }

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
                  TransactionsUpsert.this,
                  getString(R.string.transactions_categories_loaded),
                  Toast.LENGTH_SHORT
          ).show();
        });
      } catch (Exception exception) {
        runOnUiThread(() -> this.handleException(exception));
      }
    });
  }

  private void setSpTransactionType() {
    List<TransactionTypeOption> types = new ArrayList<>();
    types.add(new TransactionTypeOption(getString(R.string.transaction_type), null));
    types.add(new TransactionTypeOption(getString(R.string.income), TransactionType.INCOME));
    types.add(new TransactionTypeOption(getString(R.string.expense), TransactionType.EXPENSE));
    ArrayAdapter<TransactionTypeOption> adapter = new ArrayAdapter<>(
            this,
            R.layout.spinner_layout,
            types
    );
    adapter.setDropDownViewResource(
            R.layout.spinner_layout
    );

    Spinner spinner = findViewById(R.id.spTransactionType);
    spinner.setAdapter(adapter);
  }

  private void setFormValues() {
    Category category = transactionUpsertObject.getCategory() != null ?
            transactionUpsertObject.getCategory() :
            CacheService.get(CacheKey.CATEGORIES, transactionUpsertObject.getCategoryID(), Category.class);
    String categoryName = category != null ? category.getName() : "";
    int selected = transactionUpsertObject.getType() == TransactionType.INCOME ? 1 : 2;

    etAmount.setText(transactionUpsertObject.getAmount().toString());
    spTransactionType.setSelection(selected);
    etCategory.setText(categoryName);
    etNotes.setText(transactionUpsertObject.getNotes());
    btnUpsertTransaction.setText(getString(R.string.transactions_update));
  }

  private void handleUpsertTransaction() {
    if(transactionUpsertObject == null) this.handleInsertTransaction();
    else this.handleUpdateTransaction();
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
  private void handleInsertTransaction() {
    String amount = etAmount.getText().toString().trim();
    String notes = etNotes.getText().toString().trim();
    String categoryName = etCategory.getText().toString().trim();
    TransactionType transactionType = ((TransactionTypeOption) spTransactionType.getSelectedItem()).getType();
    progressBar.setVisibility(View.VISIBLE);
    Executors.newSingleThreadExecutor().execute(() -> {
      try {
        this.handleValidation();
        // targeting category
        List<Category> categories = CacheService.readList(CacheKey.CATEGORIES);
        Category category = categories
                .stream().
                filter(item -> categoryName.equals(item.getName()))
                .findFirst()
                .orElse(new Category(null, categoryName, authService.getUserID()));

        // creating transaction
        Transaction transaction = new Transaction(
                authService.getUserID(),
                Double.parseDouble(amount),
                category,
                transactionType,
                Timestamp.now(),
                !notes.isEmpty() ? notes : null
        );
        transactionUpsertObject =  databaseService.insertTransaction(transaction);
        runOnUiThread(() -> this.handleSuccess(getString(R.string.transaction_insert_success)));
      } catch (ValidationException exception) {
        runOnUiThread(() -> this.handleException(exception));
      } catch (Exception exception) {
        runOnUiThread(() -> this.handleException(exception));
      }
    });
  }

  private void handleUpdateTransaction() {
    String amount = etAmount.getText().toString().trim();
    String notes = etNotes.getText().toString().trim();
    String categoryName = etCategory.getText().toString().trim();
    TransactionType transactionType = ((TransactionTypeOption) spTransactionType.getSelectedItem()).getType();
    progressBar.setVisibility(View.VISIBLE);
    Executors.newSingleThreadExecutor().execute(() -> {
      try {
        this.handleValidation();
        // targeting category
        List<Category> categories = CacheService.readList(CacheKey.CATEGORIES);
        Category category = categories
                .stream().
                filter(item -> categoryName.equals(item.getName()))
                .findFirst()
                .orElse(new Category(null, categoryName, authService.getUserID()));

        // updating transaction values
        transactionUpsertObject.setAmount(Double.parseDouble(amount));
        transactionUpsertObject.setCategory(category);
        transactionUpsertObject.setType(transactionType);
        transactionUpsertObject.setNotes(!notes.isEmpty() ? notes : null);

        // saving changes to the database and immediately retrieving said object to save it to memory
        transactionUpsertObject =  databaseService.updateTransaction(transactionUpsertObject);
        runOnUiThread(() -> this.handleSuccess(getString(R.string.transaction_update_success)));
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
   *   <li>Invokes {@link #finish()} to close the current {@code TransactionsUpsert}.</li>
   * </ul>
   *
   * @see android.widget.Toast
   * @see android.view.View#GONE
   * @see #finish()
   */
  private void handleSuccess(String toastMessage) {
    Toast.makeText(
            TransactionsUpsert.this,
            toastMessage,
            Toast.LENGTH_SHORT
    ).show();
    progressBar.setVisibility(View.GONE);
    this.destroyActivity(AppCompatActivity.RESULT_OK);
  }

  private void destroyActivity(int activityResult) {
    if (transactionUpsertObject != null) {
      CacheService.store(CacheKey.TRANSACTION_UPSERT_OBJECT, transactionUpsertObject);
    }
    Intent result = new Intent();
    setResult(activityResult, result);
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
    String transactionType = ((TransactionTypeOption) spTransactionType.getSelectedItem()).getTypeAsString();

    if(amount.isEmpty()) throw new ValidationException(getString(R.string.amount_required), R.id.etAmount);
    if(Float.parseFloat(amount) == 0) throw new ValidationException(getString(R.string.amount_not_zero), R.id.etAmount);
    if(transactionType.isEmpty()) throw new ValidationException(getString(R.string.transaction_type_required), null);
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
            TransactionsUpsert.this,
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
            TransactionsUpsert.this,
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
            .setPositiveButton(R.string.discard_changes_yes, (d, which) -> destroyActivity(AppCompatActivity.RESULT_CANCELED))
            .setNegativeButton(R.string.discard_changes_no, (d, which) -> d.dismiss())
            .setCancelable(true)
            .show();
  }
}