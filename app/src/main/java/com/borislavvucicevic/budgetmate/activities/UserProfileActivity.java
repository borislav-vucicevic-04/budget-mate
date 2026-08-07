package com.borislavvucicevic.budgetmate.activities;

import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.TemplateActivity;
import com.borislavvucicevic.budgetmate.models.UserProfile;
import com.borislavvucicevic.budgetmate.enums.CurrencyCode;
import com.borislavvucicevic.budgetmate.services.LocalisationService;

import java.util.concurrent.Executors;

public class UserProfileActivity extends TemplateActivity {
  private TextView tvFullName, tvEmail, tvHomeCurrency;
  private ScrollView formWrapper;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
      Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
      return insets;
    });

    // executing the heavy task on background process
    Executors.newSingleThreadExecutor().execute(this::loadUserProfile);
  }

  @Override
  protected int getLayoutID() {
    return R.layout.activity_user_profile;
  }

  @Override
  protected void grabWidgets() {
    tvFullName = findViewById(R.id.tvFullName);
    tvEmail = findViewById(R.id.tvEmail);
    tvHomeCurrency = findViewById(R.id.tvHomeCurrency);
    formWrapper = findViewById(R.id.formWrapper);
    progressBar = findViewById(R.id.progressBar);
    localeSwitch = findViewById(R.id.localeSwitch);
  }

  @Override
  protected void setListeners() {
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
          default: /* DO NOTHING */ break;
        }

        // setting text to text views
        tvFullName.setText(userProfile.getFullName());
        tvEmail.setText(userProfile.getEmail());
        tvHomeCurrency.setText(homeCurrency);
        formWrapper.setVisibility(ScrollView.VISIBLE);
        toggleProgressBarVisibility();
      });
    } catch (Exception exception) {
      runOnUiThread(() -> this.handleException(exception, getString(R.string.error_general), UserProfileActivity.class));
    }
  }
}