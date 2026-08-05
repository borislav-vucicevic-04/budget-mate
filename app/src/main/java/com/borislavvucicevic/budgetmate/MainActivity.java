package com.borislavvucicevic.budgetmate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Spinner;
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
import com.borislavvucicevic.budgetmate.adapters.LocaleAdapter;
import com.borislavvucicevic.budgetmate.models.Category;
import com.borislavvucicevic.budgetmate.models.UserProfile;
import com.borislavvucicevic.budgetmate.enums.CacheKey;
import com.borislavvucicevic.budgetmate.services.AuthService;
import com.borislavvucicevic.budgetmate.services.CacheService;
import com.borislavvucicevic.budgetmate.services.DatabaseService;
import com.borislavvucicevic.budgetmate.services.LocalisationService;

import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
  public static final String MAIN_ACTIVITY = "MAIN_ACTIVITY";
  Button btnUserProfile;
  Button btnGenerateReports;
  Button btnViewTransactions;
  Spinner localeSwitch;
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

    this.grabWidgets();
    LocalisationService.setLocaleSwitch(localeSwitch, this);

    // Fetch categories and user  profile, and store them in cache
    this.loadCategoriesAndUserProfile();
  }

  private void grabWidgets() {
    btnUserProfile = findViewById(R.id.btnUserProfile);
    btnGenerateReports = findViewById(R.id.btnGenerateReports);
    btnViewTransactions = findViewById(R.id.btnViewTransactions);
    localeSwitch = findViewById(R.id.localeSwitch);
  }

  private void setListeners() {
    btnUserProfile.setOnClickListener(v -> this.openUserProfileActivity());
    btnViewTransactions.setOnClickListener(v -> this.openTransactionsActivity());
    btnGenerateReports.setOnClickListener(v -> this.openReportsActivity());
    localeSwitch.setOnItemSelectedListener(LocalisationService.getLocaleChangeHandler());
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