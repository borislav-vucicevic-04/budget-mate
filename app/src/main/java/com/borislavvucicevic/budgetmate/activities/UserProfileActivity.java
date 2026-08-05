package com.borislavvucicevic.budgetmate.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.models.UserProfile;
import com.borislavvucicevic.budgetmate.enums.CurrencyCode;
import com.borislavvucicevic.budgetmate.services.AuthService;
import com.borislavvucicevic.budgetmate.services.DatabaseService;
import com.borislavvucicevic.budgetmate.services.LocalisationService;

import java.util.Objects;
import java.util.concurrent.Executors;

public class UserProfileActivity extends AppCompatActivity {

  public static final String USER_PROFILE_ACTIVITY = "USER_PROFILE_ACTIVITY";
  private AuthService authService;
  private DatabaseService databaseService;
  private TextView tvFullName, tvEmail, tvHomeCurrency;
  private ScrollView formWrapper;
  private ProgressBar progressBar;
  private Spinner localeSwitch;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_user_profile);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
      Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
      return insets;
    });

    // creating instances of services
    authService = new AuthService();
    databaseService = new DatabaseService();

    // grabbing widgets
    this.grabWidgets();

    // setting listeners
    this.setListeners();
    // setting locale switch
    LocalisationService.setLocaleSwitch(localeSwitch, this);

    // executing the heavy task on background process
    Executors.newSingleThreadExecutor().execute(this::loadUserProfile);
  }

  private void grabWidgets() {
    tvFullName = findViewById(R.id.tvFullName);
    tvEmail = findViewById(R.id.tvEmail);
    tvHomeCurrency = findViewById(R.id.tvHomeCurrency);
    formWrapper = findViewById(R.id.formWrapper);
    progressBar = findViewById(R.id.progressBar);
    localeSwitch = findViewById(R.id.localeSwitch);
  }

  private void setListeners() {
    localeSwitch.setOnItemSelectedListener(LocalisationService.getLocaleChangeHandler());
  }

  private void loadUserProfile() {
    String uid = authService.getUserID();
    try {
      UserProfile userProfile = databaseService.getUserProfile(uid);
      runOnUiThread(() -> {
        // parsing homeCurrency
        CurrencyCode currencyCode = userProfile.getHomeCurrency();
        String homeCurrency = "";

        switch (currencyCode) {
          case BAM: homeCurrency = getString(R.string.currency_bam); break;
          case EUR: homeCurrency = getString(R.string.currency_eur); break;
          case RSD: homeCurrency = getString(R.string.currency_rsd); break;
          case USD: homeCurrency = getString(R.string.currency_usd); break;
          default: /* DO NOTHING */; break;
        }

        // setting text to text views
        tvFullName.setText(userProfile.getFullName());
        tvEmail.setText(userProfile.getEmail());
        tvHomeCurrency.setText(homeCurrency);
        formWrapper.setVisibility(ScrollView.VISIBLE);
        progressBar.setVisibility(View.GONE);
      });
    } catch (Exception exception) {
      runOnUiThread(() -> this.handleExceptions(exception));
    }
  }

  private void handleExceptions(Exception exception) {
    Toast.makeText(
            UserProfileActivity.this,
            getString(R.string.error_general),
            Toast.LENGTH_SHORT
    ).show();

    Log.e(USER_PROFILE_ACTIVITY, Objects.requireNonNull(exception.getMessage()));
  }
}