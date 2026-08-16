package com.borislavvucicevic.budgetmate.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
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
public class TransactionsUpsertActivity extends TemplateActivity {
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
   * User defined categories.
   *
   * <p> List of user defined categories read from cache that have previously been loaded into
   * cache when application has started.</p>
   * */
  private final List<Category> categories = CacheService.readList(CacheKey.CATEGORIES);

  /**
   * Holds the value of {@link #etAmount} widget
   * */
  private Double amount;

  /**
   * Holds the value of {@link #spTransactionType} widget
   * */
  private TransactionType type;

  /**
   * Holds category object retrieved from the cache found by
   * using the value of {@link #etCategory} widget.
   */
  private Category category;

  /**
   * Holds the value of the {@link #etNotes} widget.
   * */
  private String notes;

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
    super.grabWidgets();
    btnUpsertTransaction = findViewById(R.id.btnUpsertTransaction);
    etCategory = findViewById(R.id.etCategory);
    etAmount = findViewById(R.id.etAmount);
    etNotes = findViewById(R.id.etNotes);
    spTransactionType = findViewById(R.id.spTransactionType);
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
    super.setListeners();
    btnUpsertTransaction.setOnClickListener(v -> this.handleUpsertTransaction());
    etCategory.setOnClickListener(v -> etCategory.showDropDown());
    etCategory.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus) {
        etCategory.showDropDown();
      }
    });
  }

  /**
   * Grabs values of widgets and stores them in the class fields.
   * */
  @Override
  protected void grabValues() {
    List<Category> categories = CacheService.readList(CacheKey.CATEGORIES);
    String amountString = etAmount.getText().toString().trim();
    String categoryName = etCategory.getText().toString().trim();

    this.amount = !amountString.isEmpty() ?
            Math.abs(Double.parseDouble(amountString)) :
            null;

    this.category = !categoryName.isEmpty() ?
            // If field category is not empty, search all categories by name.
            // If there is a cached category with that name, choose it.
            // Otherwise, create new category object with ID null, meaning it must be
            // added to the database as well
            categories
              .stream()
              .filter(item -> categoryName.equals(item.getName()))
              .findFirst()
              .orElse(new Category(null, categoryName, authService.getUserID())) :
            // If category field is empty, return null meaning no category has been selected.
            null;

    this.type = ((TransactionTypeOption) spTransactionType.getSelectedItem()).getType();
    this.notes = etNotes.getText().toString().trim();
  }

  /**
   * Populates the category dropdown using the currently loaded categories.
   *
   * <p>Maps category names into an {@link ArrayAdapter}, assigns it to the
   * category field, and displays a confirmation message.</p>
   *
   * @see Category
   * @see ArrayAdapter
   */
  private void setEtCategory() {
    List<String> categoryNames = this.categories
            .stream()
            .map(Category::getName)
            .collect(Collectors.toList());

    ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            categoryNames
    );

    etCategory.setAdapter(adapter);
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

    etAmount.setText(
            String.format(
                    java.util.Locale.UK,
                    "%.2f",
                    transactionUpsertObject.getAmount()
            )
    );
    spTransactionType.setSelection(selected);
    etCategory.setText(categoryName);
    etNotes.setText(transactionUpsertObject.getNotes());
    btnUpsertTransaction.setText(getString(R.string.transactions_update));
  }

  /**
   * Inserts a new transaction or updates an existing one asynchronously.
   *
   * <p>Validates the form data in the background and performs the appropriate
   * operation based on whether {@link #transactionUpsertObject} is present.</p>
   *
   * <p>On success, displays the corresponding insert or update confirmation message.</p>
   *
   * @see #doInBackground(Runnable, Runnable)
   * @see #handleValidation()
   * @see #handleInsertTransaction()
   * @see #handleUpdateTransaction()
   */
  @WorkerThread
  private void handleUpsertTransaction() {
    boolean isInsert = transactionUpsertObject == null;
    this.grabValues();
    this.doInBackground(
            // lambda function that validates data, and then inserts new or updates
            // existing transaction based on the fact if the upsert object is null or not
            () -> {
              this.handleValidation();
              if(isInsert) this.handleInsertTransaction();
              else this.handleUpdateTransaction();
            },
            // lambda function to push updates to the ui when function executes successfully.
            () -> this.handleSuccess(getString(
                    isInsert ?
                    R.string.transaction_insert_success :
                    R.string.transaction_update_success
            ))
    );
  }

  /**
   * Creates and inserts a new transaction.
   *
   * <p>Builds the transaction from the validated form values and stores it through
   * the database service. The created transaction is assigned to
   * {@link #transactionUpsertObject}.</p>
   *
   * @see CacheService
   * @see DatabaseService
   * @see Transaction
   * @see Category
   */
  @WorkerThread
  private void handleInsertTransaction() {
    // creating transaction
    Transaction transaction = new Transaction(
            authService.getUserID(),
            this.amount,
            this.category,
            this.type,
            Timestamp.now(),
            !notes.isEmpty() ? notes : null
    );
    transactionUpsertObject =  databaseService.insertTransaction(transaction);
  }

  /**
   * Updates the existing transaction with the validated form values.
   *
   * <p>Applies the new amount, category, type, and notes, then persists the
   * updated transaction through the database service.</p>
   */
  @WorkerThread
  private void handleUpdateTransaction() {
    // updating transaction values
    transactionUpsertObject.setAmount(amount);
    transactionUpsertObject.setCategory(category);
    transactionUpsertObject.setType(type);
    transactionUpsertObject.setNotes(!notes.isEmpty() ? notes : null);

    // saving changes to the database and immediately retrieving said object to save it to memory
    transactionUpsertObject =  databaseService.updateTransaction(transactionUpsertObject);
  }

  /**
   * Handles a successful transaction insert or update.
   *
   * <p>Displays the supplied confirmation message and closes the activity with
   * {@link Activity#RESULT_OK}.</p>
   *
   * @param toastMessage the success message to display
   */
  @UiThread
  private void handleSuccess(String toastMessage) {
    this.showToast(toastMessage);
    this.destroyActivity(Activity.RESULT_OK);
  }

  /**
   * Closes the activity and returns the specified result to the calling activity.
   *
   * <p>If available, the created or updated transaction is stored in the cache
   * before the activity is finished.</p>
   *
   * @param activityResult the result code returned to the calling activity
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
   * Validates the transaction values before submission.
   *
   * <p>Checks the amount, transaction type, category, and notes, throwing a
   * {@link ValidationException} when a value is missing or invalid.</p>
   *
   * @throws ValidationException if any transaction value fails validation
   */
  private void handleValidation() {
    if(amount == null) throw new ValidationException(getString(R.string.amount_required), R.id.etAmount);
    if(amount == 0) throw new ValidationException(getString(R.string.amount_not_zero), R.id.etAmount);
    if(type == null) throw new ValidationException(getString(R.string.transaction_type_required), null);
    if(category == null) throw new ValidationException(getString(R.string.category_required), R.id.etCategory);
    if(notes.length() > 256) throw new ValidationException(getString(R.string.notes_too_long), R.id.etNotes);
  }

  /**
   * Shows a confirmation dialog before leaving the transaction form.
   *
   * <p>The user can discard the current changes and close the activity, or
   * dismiss the dialog and continue editing.</p>
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