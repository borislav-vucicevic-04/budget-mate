package com.borislavvucicevic.budgetmate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.borislavvucicevic.budgetmate.activities.LoginActivity;
import com.borislavvucicevic.budgetmate.activities.ReportsActivity;
import com.borislavvucicevic.budgetmate.activities.TransactionsActivity;
import com.borislavvucicevic.budgetmate.activities.UserProfileActivity;
import com.borislavvucicevic.budgetmate.models.classes.Category;
import com.borislavvucicevic.budgetmate.models.classes.UserProfile;
import com.borislavvucicevic.budgetmate.models.enums.CacheKey;
import com.borislavvucicevic.budgetmate.services.AuthService;
import com.borislavvucicevic.budgetmate.services.CacheService;
import com.borislavvucicevic.budgetmate.services.DatabaseService;

import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
  public static final String MAIN_ACTIVITY = "MAIN_ACTIVITY";
  private final AuthService authService = new AuthService();
  private final DatabaseService databaseService = new DatabaseService();
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_main);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activityLogin), (v, insets) -> {
      Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
      return insets;
    });

    // Checking if user is logged in. If not redirect them to LoginActivity
    if(!authService.isLoggedIn()) {
      setIntent(new Intent(getApplicationContext(), LoginActivity.class));
      finish();
    }

    // otherwise, continue execution
    // Fetch categories and user  profile, and store them in cache
    this.loadCategoriesAndUserProfile();
  }
  private void loadCategoriesAndUserProfile() {
    Toast.makeText(
            MainActivity.this,
            getString(R.string.loading_data),
            Toast.LENGTH_SHORT
    ).show();
    Executors.newSingleThreadExecutor().execute(() -> {
      try {
        // get user id
        String uid = authService.getUserID();
        // fetching categories and user profile
        List<Category> categories = databaseService.getCategories(uid);
        UserProfile userProfile = databaseService.getUserProfile(uid);
        // storing retrieved data in cache
        CacheService.store(CacheKey.CATEGORIES, categories);
        CacheService.store(CacheKey.USER_PROFILE, userProfile);
        runOnUiThread(() -> {
          // displaying toast about the action succeeding
          Toast.makeText(
                  MainActivity.this,
                  getString(R.string.data_loaded),
                  Toast.LENGTH_SHORT
          ).show();
          this.setListeners();
        });
      } catch (Exception exception) {
        runOnUiThread(() -> {
          Toast.makeText(
                  MainActivity.this,
                  getString(R.string.error_general),
                  Toast.LENGTH_SHORT
          ).show();
          Log.e(
                  MAIN_ACTIVITY,
                  exception.getMessage(),
                  exception
          );
        });
      }
    });
  }

  private void setListeners() {
    // grabbing widgets
    Button btnUserProfile = findViewById(R.id.btnUserProfile);
    Button btnGenerateReports = findViewById(R.id.btnGenerateReports);
    Button btnViewTransactions = findViewById(R.id.btnViewTransactions);
    btnUserProfile.setOnClickListener(v -> this.openUserProfileActivity());
    btnViewTransactions.setOnClickListener(v -> this.openTransactionsActivity());
    btnGenerateReports.setOnClickListener(v -> this.openReportsActivity());
  }
  private void openUserProfileActivity() {
    startActivity(new Intent(MainActivity.this, UserProfileActivity.class));
  }
  private void openTransactionsActivity() {
    startActivity(new Intent(MainActivity.this, TransactionsActivity.class));
  }
  private void openReportsActivity() {
    startActivity(new Intent(MainActivity.this, ReportsActivity.class));
  }
}