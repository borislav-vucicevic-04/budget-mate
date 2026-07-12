package com.borislavvucicevic.budgetmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.models.CurrencyCode;
import com.borislavvucicevic.budgetmate.models.CurrencyOption;
import com.borislavvucicevic.budgetmate.models.ValidationException;

import java.util.ArrayList;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {
  public static final String REGISTER_ACTIVITY = "REGISTER_ACTIVITY";
  private EditText etFullName, etEmail, etPassword, etRepeatPassword;
  private TextView tvErrorWrapper;
  private Spinner spHomeCurrency;
  private Button btnRegister;

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
    etFullName = findViewById(R.id.etFullName);
    etEmail = findViewById(R.id.etEmail);
    etPassword = findViewById(R.id.etPassword);
    etRepeatPassword = findViewById(R.id.etRepeatPassword);
    spHomeCurrency = findViewById(R.id.spHomeCurrency);
    tvErrorWrapper = findViewById(R.id.tvErrorWrapper);
    btnRegister = findViewById(R.id.btnRegister);
    TextView tvLoginLink = findViewById(R.id.tvLoginLink);

    // setting event handlers
    btnRegister.setOnClickListener(v -> this.handleRegistration());
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
  private void handleRegistration() {
    try {
      this.validateForm();
    } catch(ValidationException exception) {
      this.handleRegistrationExceptions(exception);
      if(exception.getViewID() != null) {
        ((EditText) findViewById(exception.getViewID())).setError(exception.getMessage());
      }
    }
  }
  private void handleRegistrationExceptions(Exception exception) {
    Log.e(REGISTER_ACTIVITY, exception.getMessage(), exception);
    tvErrorWrapper.setVisibility(TextView.VISIBLE);
    tvErrorWrapper.setText(exception.getMessage());
  }
  private void handleLoginLink() {
    startActivity(new Intent(getApplicationContext(), LoginActivity.class));
  }
  /**
   * Validates the user input fields within the registration form.
   * This method extracts data from the full name, email, password, and repeat password
   * input fields, as well as the home currency selection spinner. It performs presence
   * and integrity constraints checking sequentially. If any field fails its validation rule,
   * the method immediately stops execution and routes the error message and failing view ID
   * via a custom exception.
   *
   * @throws ValidationException If any input field is empty, if the password is under 6 characters,
   * or if the confirmation password does not match the chosen password.
   * */
  private void validateForm() throws ValidationException {
    String fullName = etFullName.getText().toString().trim();
    String email = etEmail.getText().toString().trim();
    String password = etPassword.getText().toString();
    String repeatPassword = etRepeatPassword.getText().toString();
    String homeCurrency = spHomeCurrency.getSelectedItem().toString();

    if(fullName.isEmpty()) throw new ValidationException(getString(R.string.full_name_required), R.id.etFullName);
    if(email.isEmpty()) throw new ValidationException(getString(R.string.email_required), R.id.etEmail);
    if(homeCurrency.isEmpty()) throw new ValidationException(getString(R.string.home_currency_required), null);
    if(password.isEmpty()) throw new ValidationException(getString(R.string.password_required), R.id.etPassword);
    if(password.length() < 6) throw new ValidationException(getString(R.string.password_too_short), R.id.etPassword);
    if(repeatPassword.isEmpty()) throw new ValidationException(getString(R.string.repeat_password_required), R.id.etRepeatPassword);
    if(!repeatPassword.equals(password)) throw new ValidationException(getString(R.string.password_mismatch), R.id.etRepeatPassword);
  }
}