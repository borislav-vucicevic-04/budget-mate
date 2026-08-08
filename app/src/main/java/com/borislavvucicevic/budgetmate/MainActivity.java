package com.borislavvucicevic.budgetmate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.borislavvucicevic.budgetmate.activities.LoginActivity;
import com.borislavvucicevic.budgetmate.activities.ReportsActivity;
import com.borislavvucicevic.budgetmate.activities.TransactionsActivity;
import com.borislavvucicevic.budgetmate.activities.UserProfileActivity;
import com.borislavvucicevic.budgetmate.models.Category;
import com.borislavvucicevic.budgetmate.models.UserProfile;
import com.borislavvucicevic.budgetmate.enums.CacheKey;
import com.borislavvucicevic.budgetmate.services.CacheService;
import com.borislavvucicevic.budgetmate.services.LocalisationService;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * Main activity of the BudgetMate application.
 *
 * <p>This activity acts as the main navigation screen after a user has
 * successfully authenticated. It provides access to the user's profile,
 * transaction management, and financial reports.</p>
 *
 * <p>When the activity is created, it verifies that the user is logged in.
 * If no authenticated user exists, the user is redirected to
 * {@link LoginActivity}. Otherwise, the application loads the user's
 * categories and profile information from the database and stores them
 * in {@link CacheService} for later use.</p>
 *
 * <p>The activity also supports changing the application's locale through
 * the locale switch inherited from {@link TemplateActivity}.</p>
 *
 * @see TemplateActivity
 * @see LoginActivity
 * @see UserProfileActivity
 * @see TransactionsActivity
 * @see ReportsActivity
 */
public class MainActivity extends TemplateActivity {
  /**
   * Button used to navigate to the user profile screen.
   */
  private Button btnUserProfile;

  /**
   * Button used to navigate to the reports screen.
   */
  private Button btnGenerateReports;

  /**
   * Button used to navigate to the transactions screen.
   */
  private Button btnViewTransactions;

  /**
   * Called when the activity is first created.
   *
   * <p>This method enables edge-to-edge display, configures system-bar
   * insets, verifies that the user is authenticated, and starts loading
   * the user's categories and profile information.</p>
   *
   * <p>If the user is not logged in, the activity is closed and the user
   * is redirected to {@link LoginActivity}.</p>
   *
   * @param savedInstanceState previously saved activity state, or
   *                           {@code null} if the activity is being
   *                           created for the first time
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    EdgeToEdge.enable(this);

    ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.activityLogin),
            (v, insets) -> {
              Insets systemBars =
                      insets.getInsets(WindowInsetsCompat.Type.systemBars());

              v.setPadding(
                      systemBars.left,
                      systemBars.top,
                      systemBars.right,
                      systemBars.bottom
              );

              return insets;
            }
    );

    // Checking if user is logged in.
    // If not, redirect them to LoginActivity.
    if (!authService.isSignedIn()) {
      startActivity(new Intent(getApplicationContext(), LoginActivity.class));
      finish();
    }

    // Fetch categories and user profile and store them in cache.
    this.loadCategoriesAndUserProfile();
  }

  /**
   * Returns the layout resource used by this activity.
   *
   * <p>The returned layout is used by {@link TemplateActivity} when
   * initializing the activity interface.</p>
   *
   * @return resource identifier of the main activity layout
   */
  @Override
  protected int getLayoutID() {
    return R.layout.activity_main;
  }

  /**
   * Retrieves and stores references to the user-interface widgets
   * contained in the activity layout.
   *
   * <p>This includes the profile, reports, and transactions buttons,
   * as well as the locale switch inherited from
   * {@link TemplateActivity}.</p>
   */
  @Override
  protected void grabWidgets() {
    btnUserProfile = findViewById(R.id.btnUserProfile);
    btnGenerateReports = findViewById(R.id.btnGenerateReports);
    btnViewTransactions = findViewById(R.id.btnViewTransactions);
    localeSwitch = findViewById(R.id.localeSwitch);
  }

  /**
   * Registers event listeners for the activity's interactive widgets.
   *
   * <p>The navigation buttons open their corresponding activities,
   * while the locale switch uses the locale change handler provided
   * by {@link LocalisationService}.</p>
   */
  @Override
  protected void setListeners() {
    btnUserProfile.setOnClickListener(v -> this.openActivity(UserProfileActivity.class));
    btnViewTransactions.setOnClickListener(v -> this.openActivity(TransactionsActivity.class));
    btnGenerateReports.setOnClickListener(v -> this.openActivity(ReportsActivity.class));
    localeSwitch.setOnItemSelectedListener(LocalisationService.getLocaleChangeHandler());
  }

  private void toggleButtonsInteractivity() {
    btnUserProfile.setEnabled(!btnUserProfile.isEnabled());
    btnGenerateReports.setEnabled(!btnGenerateReports.isEnabled());
    btnViewTransactions.setEnabled(!btnViewTransactions.isEnabled());
  }

  /**
   * Loads the authenticated user's categories and profile information
   * from the database.
   *
   * <p>The operation is performed on a background thread to avoid
   * blocking the Android UI thread. Before loading starts, all buttons
   * are disabled, preventing navigation
   * to other application screens while the required data is being
   * retrieved.</p>
   *
   * <p>After successful retrieval, the categories and user profile are
   * stored in {@link CacheService} using {@link CacheKey#CATEGORIES}
   * and {@link CacheKey#USER_PROFILE}. A success message is then shown
   * to the user.</p>
   */
  private void loadCategoriesAndUserProfile() {
    this.toggleButtonsInteractivity();
    this.showToast(getString(R.string.loading_data));

    Executors.newSingleThreadExecutor().execute(() -> {
      try {
        // Get user ID.
        String uid = authService.getUserID();

        // Fetch categories and user profile.
        List<Category> categories = databaseService.getCategories(uid);
        UserProfile userProfile = databaseService.getUserProfile(uid);

        // Store retrieved data in cache.
        CacheService.store(CacheKey.CATEGORIES, categories);
        CacheService.store(CacheKey.USER_PROFILE, userProfile);
        Log.d("MainActivity-DEBUG", "UserProfile: " + userProfile);
        Log.d("MainActivity-DEBUG", "Categories: " + categories);
        Thread.sleep(2000);
        runOnUiThread(() -> {
          this.toggleButtonsInteractivity();
          // Display success message.
          this.showToast(getString(R.string.data_loaded));
        });

      }
      catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Thread was interrupted", exception);
      }
      catch (Exception exception) {
        runOnUiThread(() -> this.handleException(
                  exception,
                  getString(R.string.error_general),
                  MainActivity.class
        ));
      }
    });
  }
}