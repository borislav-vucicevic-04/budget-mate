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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.models.classes.Category;
import com.borislavvucicevic.budgetmate.models.exceptions.ValidationException;
import com.borislavvucicevic.budgetmate.services.AuthService;
import com.borislavvucicevic.budgetmate.services.DatabaseService;

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

  private void handleCreateTransaction() {
    progressBar.setVisibility(View.VISIBLE);
    Executors.newSingleThreadExecutor().execute(() -> {
      try {
        this.handleValidation();
      } catch (ValidationException exception) {
        runOnUiThread(() -> this.handleException(exception));
      } catch (Exception exception) {
        runOnUiThread(() -> this.handleException(exception));
      }
    });
  }

  private void handleValidation() {
    String amount = etAmount.getText().toString().trim();
    String category = etCategory.getText().toString().trim();
    String notes = etNotes.getText().toString().trim();

    if(amount.isEmpty()) throw new ValidationException(getString(R.string.amount_required), R.id.etAmount);
    if(Float.parseFloat(amount) == 0) throw new ValidationException(getString(R.string.amount_not_zero), R.id.etAmount);
    if(category.isEmpty()) throw new ValidationException(getString(R.string.category_required), R.id.etCategory);
    if(notes.length() > 256) throw new ValidationException(getString(R.string.notes_too_long), R.id.etNotes);
  }

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
}