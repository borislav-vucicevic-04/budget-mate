package com.borislavvucicevic.budgetmate.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.TemplateActivity;
import com.borislavvucicevic.budgetmate.models.Category;
import com.borislavvucicevic.budgetmate.models.Transaction;
import com.borislavvucicevic.budgetmate.options.TransactionTypeOption;
import com.borislavvucicevic.budgetmate.enums.CacheKey;
import com.borislavvucicevic.budgetmate.enums.TransactionType;
import com.borislavvucicevic.budgetmate.exceptions.ValidationException;
import com.borislavvucicevic.budgetmate.services.CacheService;
import com.borislavvucicevic.budgetmate.services.DatabaseService;
import com.borislavvucicevic.budgetmate.services.LocalisationService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Activity responsible for creating and updating financial transactions
 * in the BudgetMate application.
 *
 * <p>This activity provides a form that allows the user to enter or modify
 * a transaction's amount, transaction type, category, and optional notes.
 * The activity supports both insertion of new transactions and editing of
 * existing transactions.</p>
 *
 * <p>When an existing transaction is available through
 * {@link CacheService}, the activity operates in update mode and populates
 * the form with the transaction's current values. Otherwise, the activity
 * operates in insertion mode.</p>
 *
 * <p>Categories are displayed through an {@link AutoCompleteTextView}, while
 * transaction types are presented using a {@link Spinner}. Database
 * operations are executed asynchronously to avoid blocking the Android UI
 * thread.</p>
 *
 * <p>After a successful insert or update operation, the resulting
 * {@link Transaction} is stored temporarily in {@link CacheService} and
 * returned to the calling activity through an activity result.</p>
 *
 * <p>The activity also intercepts the system back action and asks the user
 * to confirm whether changes should be discarded before closing the form.</p>
 *
 * @see TemplateActivity
 * @see TransactionsActivity
 * @see Transaction
 * @see Category
 * @see CacheService
 * @see DatabaseService
 */
public class TransactionsUpsert extends TemplateActivity {
  /**
   * Auto-complete input field used to select or enter the transaction category.
   */
  private AutoCompleteTextView etCategory;

  /**
   * Input field used to enter the monetary amount of the transaction.
   */
  private EditText etAmount;

  /**
   * Input field used to enter optional notes associated with the transaction.
   */
  private EditText etNotes;

  /**
   * Spinner used to select whether the transaction represents income or
   * an expense.
   */
  private Spinner spTransactionType;

  /**
   * Button used to create a new transaction or save changes to an existing
   * transaction.
   */
  private Button btnUpsertTransaction;

  /**
   * Transaction currently being created or edited.
   *
   * <p>If a transaction exists under
   * {@link CacheKey#TRANSACTION_UPSERT_OBJECT}, the activity operates in
   * update mode. If no transaction is found, the activity operates in
   * insert mode.</p>
   */
  private Transaction transactionUpsertObject = CacheService.read(CacheKey.TRANSACTION_UPSERT_OBJECT, Transaction.class);

  /**
   * Called when the transaction upsert activity is first created.
   *
   * <p>This method enables edge-to-edge rendering and adjusts the activity
   * layout to account for the on-screen keyboard.</p>
   *
   * <p>The category auto-complete field and transaction-type spinner are
   * initialized. If an existing transaction is being edited, its values are
   * loaded into the form using {@link #setFormValues()}.</p>
   *
   * <p>A custom back-press callback is also registered so that attempting
   * to leave the activity displays a confirmation dialog before any changes
   * are discarded.</p>
   *
   * @param savedInstanceState previously saved activity state, or
   *                           {@code null} if the activity is being created
   *                           for the first time
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
      int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
      v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), imeBottom);
      return insets;
    });

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
   * Returns the layout resource used by this activity.
   *
   * @return resource identifier of the transaction upsert activity layout
   */
  @Override
  protected int getLayoutID() {
    return R.layout.activity_transactions_upsert;
  }

  /**
   * Retrieves and stores references to the user-interface widgets contained
   * in the transaction upsert activity layout.
   *
   * <p>This includes the transaction amount, category, notes, transaction-type
   * spinner, submit button, error wrapper, progress indicator, and locale
   * selector.</p>
   */
  @Override
  protected void grabWidgets() {
    btnUpsertTransaction = findViewById(R.id.btnUpsertTransaction);
    etCategory = findViewById(R.id.etCategory);
    tvErrorWrapper = findViewById(R.id.tvErrorWrapper);
    etAmount = findViewById(R.id.etAmount);
    etNotes = findViewById(R.id.etNotes);
    spTransactionType = findViewById(R.id.spTransactionType);
    progressBar = findViewById(R.id.progressBar);
    localeSwitch = findViewById(R.id.localeSwitch);
  }

  /**
   * Registers event listeners for the interactive controls on the transaction
   * form.
   *
   * <p>The submit button initiates the insert or update operation. The
   * category auto-complete dropdown is displayed whenever the category field
   * receives focus or is clicked.</p>
   *
   * <p>The locale selector uses the language change handler provided by
   * {@link LocalisationService}.</p>
   */
  @Override
  protected void setListeners() {
    btnUpsertTransaction.setOnClickListener(v -> this.handleUpsertTransaction());
    etCategory.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus) {
        etCategory.showDropDown();
      }
    });
    etCategory.setOnClickListener(v -> etCategory.showDropDown());
    localeSwitch.setOnItemSelectedListener(LocalisationService.getLocaleChangeHandler());
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
   * back to the main thread via handleException(Exception).
   * </p>
   *
   * @see Category
   * @see ArrayAdapter
   * @see DatabaseService
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
          this.showToast(getString(R.string.transactions_categories_loaded));
        });
      } catch (Exception exception) {
        runOnUiThread(() -> this.handleException(exception, getString(R.string.error_general), TransactionsUpsert.class));
      }
    });
  }

  /**
   * Initializes and configures the transaction-type spinner.
   *
   * <p>The spinner contains a default placeholder option followed by the
   * {@link TransactionType#INCOME} and {@link TransactionType#EXPENSE}
   * transaction types. Each displayed label is wrapped in a
   * {@link TransactionTypeOption} containing the corresponding enum value.</p>
   *
   * <p>An {@link ArrayAdapter} using the application's custom spinner layout
   * is created and attached to the transaction-type spinner.</p>
   *
   * @see TransactionType
   * @see TransactionTypeOption
   */
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

  /**
   * Populates the transaction form with values from the transaction currently
   * being edited.
   *
   * <p>The method resolves the transaction's category either directly from
   * the transaction object or from {@link CacheService} using the stored
   * category identifier.</p>
   *
   * <p>The amount, transaction type, category name, and notes are then
   * displayed in their corresponding form controls. The submit button text
   * is also changed to indicate that the operation will update an existing
   * transaction.</p>
   */
  private void setFormValues() {
    Category category = transactionUpsertObject.getCategory() != null ?
            transactionUpsertObject.getCategory() :
            CacheService.get(CacheKey.CATEGORIES, transactionUpsertObject.getCategoryID(), Category.class);
    String categoryName = category != null ? category.getName() : "";
    int selected = transactionUpsertObject.getType() == TransactionType.INCOME ? 1 : 2;

    etAmount.setText(String.format("%.2f", transactionUpsertObject.getAmount()));
    spTransactionType.setSelection(selected);
    etCategory.setText(categoryName);
    etNotes.setText(transactionUpsertObject.getNotes());
    btnUpsertTransaction.setText(getString(R.string.transactions_update));
  }

  /**
   * Determines whether the current operation should insert a new transaction
   * or update an existing transaction.
   *
   * <p>If {@link #transactionUpsertObject} is {@code null}, the activity is
   * operating in creation mode and {@link #handleInsertTransaction()} is
   * called. Otherwise, {@link #handleUpdateTransaction()} is called.</p>
   */
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
   * <p>The selected category name is matched against categories stored in
   * {@link CacheService}. If no matching category exists, a new
   * {@link Category} object is created for the entered category name.</p>
   *
   * <p>After validation succeeds, a new {@link Transaction} is created using
   * the authenticated user's identifier, amount, category, transaction type,
   * current timestamp, and optional notes. The transaction is then inserted
   * through the database service.</p>
   *
   * <p>Successful insertion is forwarded to
   * {@link #handleSuccess(String)} on the main UI thread. Validation and
   * unexpected exceptions are forwarded to the appropriate exception
   * handlers.</p>
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

    toggleProgressBarVisibility();

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
        runOnUiThread(() -> this.handleException(exception, TransactionsActivity.class));
      } catch (Exception exception) {
        runOnUiThread(() -> this.handleException(exception, getString(R.string.error_general), TransactionsUpsert.class));
      }
    });
  }

  /**
   * Handles the asynchronous update of an existing financial transaction.
   *
   * <p>The method retrieves the current amount, notes, category name, and
   * transaction type from the form and displays the loading indicator.</p>
   *
   * <p>After validation succeeds, the selected category is resolved from
   * {@link CacheService}. If no matching category exists, a new category
   * object is created using the entered category name.</p>
   *
   * <p>The existing {@link #transactionUpsertObject} is updated with the new
   * amount, category, transaction type, and optional notes. The modified
   * transaction is then persisted through {@link DatabaseService}.</p>
   *
   * <p>After the database operation succeeds, the updated transaction returned
   * by the database service replaces the local transaction object and
   * {@link #handleSuccess(String)} is called on the main UI thread.</p>
   *
   * <p>Validation and unexpected exceptions are forwarded to the appropriate
   * exception handlers.</p>
   *
   * @see Transaction
   * @see Category
   * @see DatabaseService
   */
  private void handleUpdateTransaction() {
    String amount = etAmount.getText().toString().trim();
    String notes = etNotes.getText().toString().trim();
    String categoryName = etCategory.getText().toString().trim();
    TransactionType transactionType = ((TransactionTypeOption) spTransactionType.getSelectedItem()).getType();

    toggleProgressBarVisibility();

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
        runOnUiThread(() -> this.handleException(exception, TransactionsUpsert.class));
      } catch (Exception exception) {
        runOnUiThread(() -> this.handleException(exception, getString(R.string.error_general), TransactionsUpsert.class));
      }
    });
  }

  /**
   * Finalizes a successful transaction insert or update operation.
   * <p>
   * This method provides visual feedback to the user, stops active loading animations,
   * and navigates the user back to the previous screen by popping this Activity off the application stack.
   * </p>
   *
   * <p><b>UI Actions Performed:</b></p>
   * <ul>
   *   <li>Displays a short-duration {@link Toast} message notifying the user that the transaction was successfully saved.</li>
   *   <li>Hides the active {@code progressBar} by toggling its visibility.</li>
   *   <li>Returns {@link AppCompatActivity#RESULT_OK} to the calling activity.</li>
   * </ul>
   *
   * @param toastMessage localized message displayed after the transaction
   *                     has been successfully saved
   *
   * @see android.widget.Toast
   * @see #destroyActivity(int)
   */
  private void handleSuccess(String toastMessage) {
    this.showToast(toastMessage);
    this.toggleProgressBarVisibility();
    this.destroyActivity(Activity.RESULT_OK);
  }

  /**
   * Closes the current activity and returns the specified result code to the
   * calling activity.
   *
   * <p>If a transaction object is available, it is stored under
   * {@link CacheKey#TRANSACTION_UPSERT_OBJECT} so that the calling
   * {@link TransactionsActivity} can retrieve the newly created or updated
   * transaction.</p>
   *
   * <p>The supplied result code is assigned using
   * {@link #setResult(int, Intent)} before the activity is finished.</p>
   *
   * @param activityResult result code to return to the calling activity,
   *                       typically {@link AppCompatActivity#RESULT_OK} or
   *                       {@link AppCompatActivity#RESULT_CANCELED}
   */
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
   *   <li><b>Transaction Type Required:</b> Ensures that an income or expense type has been selected.</li>
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
   * Displays a confirmation dialog when the user attempts to leave the
   * transaction form.
   *
   * <p>The dialog asks whether the current changes should be discarded.
   * Selecting the positive action closes the activity with
   * {@link AppCompatActivity#RESULT_CANCELED}. Selecting the negative action
   * dismisses the dialog and allows the user to continue editing.</p>
   *
   * <p>The dialog is cancelable, allowing the user to dismiss it without
   * leaving the activity.</p>
   */
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