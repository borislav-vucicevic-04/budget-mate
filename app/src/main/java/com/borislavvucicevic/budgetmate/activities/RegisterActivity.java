package com.borislavvucicevic.budgetmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.models.CurrencyCode;
import com.borislavvucicevic.budgetmate.models.CurrencyOption;

import java.util.ArrayList;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_register);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_register), (v, insets) -> {
      int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
      v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), imeBottom);
      return insets;
    });
    // Setting up the currency spinner
    this.setupCurrencySpinner();

    // getting widgets
    TextView tvLoginLink = findViewById(R.id.tvLoginLink);

    // setting event handlers
    tvLoginLink.setOnClickListener(v -> this.handleLoginLink());
  }
  /**
   * Initializes and configures the currency selection spinner.
   * This method creates a list of supported currency options, including a default
   * home currency and specific international currencies (BAM, RSD, EUR, USD).
   * It binds this data to an {@link android.widget.ArrayAdapter} using default
   * Android spinner layouts and attaches the adapter to the {@code spHomeCurrency} view component.
   * */
  private void setupCurrencySpinner() {
    List<CurrencyOption> currencies = new ArrayList<>();
    currencies.add(new CurrencyOption(getString(R.string.home_currency), null));
    currencies.add(new CurrencyOption(getString(R.string.currency_bam), CurrencyCode.BAM));
    currencies.add(new CurrencyOption(getString(R.string.currency_rsd), CurrencyCode.RSD));
    currencies.add(new CurrencyOption(getString(R.string.currency_eur), CurrencyCode.EUR));
    currencies.add(new CurrencyOption(getString(R.string.currency_usd), CurrencyCode.USD));
    ArrayAdapter<CurrencyOption> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            currencies
    );
    adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
    );

    Spinner spinner = findViewById(R.id.spHomeCurrency);
    spinner.setAdapter(adapter);
  }
  private void handleLoginLink() {
    startActivity(new Intent(getApplicationContext(), LoginActivity.class));
  }
}