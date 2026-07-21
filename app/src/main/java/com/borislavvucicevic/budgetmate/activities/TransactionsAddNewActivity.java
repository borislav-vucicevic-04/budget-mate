package com.borislavvucicevic.budgetmate.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.models.classes.Category;
import com.borislavvucicevic.budgetmate.services.AuthService;
import com.borislavvucicevic.budgetmate.services.DatabaseService;

import java.util.List;
import java.util.concurrent.Executors;

public class TransactionsAddNewActivity extends AppCompatActivity {
  public static final String TRANSACTION_ADD_NEW_ACTIVITY = "TRANSACTION_ADD_NEW_ACTIVITY";
  private AuthService authService;
  private DatabaseService databaseService;
  private AutoCompleteTextView etCategory;
  private TextView tvErrorWrapper;
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
    etCategory = findViewById(R.id.etCategory);
    tvErrorWrapper = findViewById(R.id.tvErrorWrapper);
    // setting listeners
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
        List<String> categoryNames = categories.stream().map(Category::getName).toList();
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