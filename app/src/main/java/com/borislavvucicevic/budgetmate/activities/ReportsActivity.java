package com.borislavvucicevic.budgetmate.activities;

import android.content.Context;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.TemplateActivity;
import com.borislavvucicevic.budgetmate.enums.CacheKey;
import com.borislavvucicevic.budgetmate.enums.CurrencyCode;
import com.borislavvucicevic.budgetmate.exceptions.ValidationException;
import com.borislavvucicevic.budgetmate.models.CustomReport;
import com.borislavvucicevic.budgetmate.models.DailyReport;
import com.borislavvucicevic.budgetmate.models.MonthlyReport;
import com.borislavvucicevic.budgetmate.models.QuarterlyReport;
import com.borislavvucicevic.budgetmate.models.Report;
import com.borislavvucicevic.budgetmate.models.Transaction;
import com.borislavvucicevic.budgetmate.models.UserProfile;
import com.borislavvucicevic.budgetmate.models.YearlyReport;
import com.borislavvucicevic.budgetmate.options.MonthOption;
import com.borislavvucicevic.budgetmate.options.QuarterOption;
import com.borislavvucicevic.budgetmate.options.ReportTypeOption;
import com.borislavvucicevic.budgetmate.enums.Month;
import com.borislavvucicevic.budgetmate.enums.Quarter;
import com.borislavvucicevic.budgetmate.enums.ReportType;
import com.borislavvucicevic.budgetmate.services.CacheService;
import com.borislavvucicevic.budgetmate.services.LocalisationService;
import com.borislavvucicevic.budgetmate.services.ReportHtmlService;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.Timestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

public class ReportsActivity extends TemplateActivity {
  public static final String REPORTS_ACTIVITY = "REPORTS_ACTIVITY";
  private Spinner spReportType, spMonth, spQuarter;
  private EditText etYear;
  private AppCompatTextView dpDate, dpCustomRangeFrom, dpCustomRangeTo;
  private Button btnGenerate;
  private WebView webView;
  private LocalDate dpDateValue, dpCustomRangeFromValue, dpCustomRangeToValue;
  private ReportType reportType = ReportType.DAILY;
  private String etYearValue;
  private Month spMonthValue = Month.JAN;
  private Quarter spQuarterValue = Quarter.I;
  private Timestamp from, to;
  private String reportHtml;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.reportsMain), (v, insets) -> {
      int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
      v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), imeBottom);
      return insets;
    });

    // setting up spinners
    this.setSpReportType();
    this.setSpQuarter();
    this.setSpMonth();
  }

  @Override
  protected int getLayoutID() {
    return R.layout.activity_reports;
  }


  @Override
  protected void grabWidgets() {
    spReportType = findViewById(R.id.spReportType);
    spMonth = findViewById(R.id.spMonth);
    spQuarter = findViewById(R.id.spQuarter);
    localeSwitch = findViewById(R.id.localeSwitch);
    dpDate = findViewById(R.id.dpDate);
    etYear = findViewById(R.id.etYear);
    dpCustomRangeFrom = findViewById(R.id.dpCustomRangeFrom);
    dpCustomRangeTo = findViewById(R.id.dpCustomRangeTo);
    tvErrorWrapper = findViewById(R.id.tvErrorWrapper);
    progressBar = findViewById(R.id.progressBar);
    btnGenerate = findViewById(R.id.btnGenerate);
    webView = findViewById(R.id.webView);
  }

  protected void setListeners() {
    spReportType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // Cast the item directly to your custom object type
        ReportTypeOption reportTypeOption = (ReportTypeOption) parent.getItemAtPosition(position);
        reportType = reportTypeOption.getType();
        displayFields();
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        // DO NOTHING
      }
    });
    spMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        MonthOption monthOption = (MonthOption) parent.getItemAtPosition(position);
        spMonthValue = monthOption.getMonth();
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        // DO NOTHING
      }
    });
    spQuarter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        QuarterOption quarterOption = (QuarterOption) parent.getItemAtPosition(position);
        spQuarterValue = quarterOption.getQuarter();
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        // DO NOTHING
      }
    });
    localeSwitch.setOnItemSelectedListener(LocalisationService.getLocaleChangeHandler());
    dpDate.setOnClickListener(v -> showDatePicker(dpDateValue, v.getId()));
    dpCustomRangeFrom.setOnClickListener(v -> showDatePicker(dpCustomRangeFromValue, v.getId()));
    dpCustomRangeTo.setOnClickListener(v -> showDatePicker(dpCustomRangeToValue, v.getId()));
    btnGenerate.setOnClickListener(v -> handleGenerateReport());
    webView.setWebViewClient(new WebViewClient() {
      @Override
      public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        String documentTitle = view.getTitle();

        // 2. Fallback to a default name if the HTML has no <title> tag
        if (documentTitle == null || documentTitle.trim().isEmpty()) {
          documentTitle = getString(R.string.report_generic_document_name);
        }

        // 3. Clean up the title (removes characters that might break file names)
        documentTitle = documentTitle.replaceAll("[\\\\/:*?\"<>|]", "_");

        // 4. Pass the custom title to your print method

        triggerWebViewPrint(view, documentTitle);
      }
    });
  }

  /**
   * Initializes the report-type spinner with all available report type options.
   *
   * <p>Each displayed, localized label is associated with its corresponding
   * {@link ReportType} value. The configured adapter is then assigned to
   * {@code spReportType}.</p>
   */
  private void setSpReportType() {
    List<ReportTypeOption> reportTypeOptions = new ArrayList<>();
    reportTypeOptions.add(
            new ReportTypeOption(getString(R.string.report_daily), ReportType.DAILY));
    reportTypeOptions.add(
            new ReportTypeOption(getString(R.string.report_monthly), ReportType.MONTHLY));
    reportTypeOptions.add(
            new ReportTypeOption(getString(R.string.report_quarterly), ReportType.QUARTERLY));
    reportTypeOptions.add(
            new ReportTypeOption(getString(R.string.report_yearly), ReportType.YEARLY));
    reportTypeOptions.add(
            new ReportTypeOption(getString(R.string.report_custom), ReportType.CUSTOM));

    ArrayAdapter<ReportTypeOption> adapter = new ArrayAdapter<>(
            this,
            R.layout.spinner_layout,
            reportTypeOptions
    );
    adapter.setDropDownViewResource(R.layout.spinner_layout);

    spReportType.setAdapter(adapter);
  }

  /**
   * Initializes the quarter spinner with the four available quarters.
   *
   * <p>Each displayed, localized label is associated with its corresponding
   * {@link Quarter} value. The configured adapter is then assigned to
   * {@code spQuarter}.</p>
   */
  private void setSpQuarter() {
    List<QuarterOption> quarterOptions = new ArrayList<>();
    quarterOptions.add(
            new QuarterOption(getString(R.string.reports_quarter_first), Quarter.I));
    quarterOptions.add(
            new QuarterOption(getString(R.string.report_quarter_second), Quarter.II));
    quarterOptions.add(
            new QuarterOption(getString(R.string.report_quarter_third), Quarter.III));
    quarterOptions.add(
            new QuarterOption(getString(R.string.report_quarter_fourth), Quarter.IV));

    ArrayAdapter<QuarterOption> adapter = new ArrayAdapter<>(
            this,
            R.layout.spinner_layout,
            quarterOptions
    );
    adapter.setDropDownViewResource(R.layout.spinner_layout);

    spQuarter.setAdapter(adapter);
  }

  /**
   * Initializes the month spinner with all twelve months of the year.
   *
   * <p>Each displayed, localized month label is associated with its corresponding
   * {@link Month} value. The configured adapter is then assigned to
   * {@code spMonth}.</p>
   */
  private void setSpMonth() {
    List<MonthOption> monthOptions = new ArrayList<>();
    monthOptions.add(new MonthOption(getString(R.string.jan), Month.JAN));
    monthOptions.add(new MonthOption(getString(R.string.feb), Month.FEB));
    monthOptions.add(new MonthOption(getString(R.string.mar), Month.MAR));
    monthOptions.add(new MonthOption(getString(R.string.apr), Month.APR));
    monthOptions.add(new MonthOption(getString(R.string.may), Month.MAY));
    monthOptions.add(new MonthOption(getString(R.string.jun), Month.JUN));
    monthOptions.add(new MonthOption(getString(R.string.jul), Month.JUL));
    monthOptions.add(new MonthOption(getString(R.string.aug), Month.AUG));
    monthOptions.add(new MonthOption(getString(R.string.sep), Month.SEP));
    monthOptions.add(new MonthOption(getString(R.string.oct), Month.OCT));
    monthOptions.add(new MonthOption(getString(R.string.nov), Month.NOV));
    monthOptions.add(new MonthOption(getString(R.string.dec), Month.DEC));

    ArrayAdapter<MonthOption> adapter = new ArrayAdapter<>(
            this,
            R.layout.spinner_layout,
            monthOptions
    );
    adapter.setDropDownViewResource(R.layout.spinner_layout);

    spMonth.setAdapter(adapter);
  }

  /**
   * Displays the input fields required for the specified report type.
   *
   * <p>All report-related input fields are hidden before the fields associated
   * with the selected report type are made visible.</p>
   *
   * <ul>
   *   <li>{@link ReportType#DAILY}: displays the date field.</li>
   *   <li>{@link ReportType#MONTHLY}: displays the month and year fields.</li>
   *   <li>{@link ReportType#QUARTERLY}: displays the quarter and year fields.</li>
   *   <li>{@link ReportType#YEARLY}: displays the year field.</li>
   *   <li>{@link ReportType#CUSTOM}: displays the start-date and end-date fields.</li>
   * </ul>
   */
  private void displayFields() {
    this.hideFields();
    switch (reportType) {
      case DAILY:
        dpDate.setVisibility(View.VISIBLE);
        break;
      case MONTHLY:
        spMonth.setVisibility(View.VISIBLE);
        etYear.setVisibility(View.VISIBLE);
        break;
      case QUARTERLY:
        spQuarter.setVisibility(View.VISIBLE);
        etYear.setVisibility(View.VISIBLE);
        break;
      case YEARLY:
        etYear.setVisibility(View.VISIBLE);
        break;
      case CUSTOM:
        dpCustomRangeFrom.setVisibility(View.VISIBLE);
        dpCustomRangeTo.setVisibility(View.VISIBLE);
        break;
      default:
        Log.d(REPORTS_ACTIVITY, "For some reason no field has been displayed.");
    }
  }

  /**
   * Hides all input fields used to configure a report period.
   *
   * <p>This includes the month spinner, quarter spinner, date field, year field,
   * and both custom date-range fields.</p>
   */
  private void hideFields() {
    spMonth.setVisibility(View.GONE);
    spQuarter.setVisibility(View.GONE);
    dpDate.setVisibility(View.GONE);
    etYear.setVisibility(View.GONE);
    dpCustomRangeFrom.setVisibility(View.GONE);
    dpCustomRangeTo.setVisibility(View.GONE);
  }

  /**
   * Enables all report configuration fields and controls.
   *
   * <p>This method restores user interaction with the report type, month,
   * quarter, date, year and custom date-range fields. It also enables the
   * error wrapper, progress indicator and report generation button.</p>
   */
  private void enableFields() {
    spReportType.setEnabled(true);
    spReportType.setEnabled(true);
    spMonth.setEnabled(true);
    spQuarter.setEnabled(true);
    dpDate.setEnabled(true);
    etYear.setEnabled(true);
    dpCustomRangeFrom.setEnabled(true);
    dpCustomRangeTo.setEnabled(true);
    btnGenerate.setEnabled(true);
  }

  /**
   * Disables all report configuration fields and controls.
   *
   * <p>This method prevents user interaction with the report type, month,
   * quarter, date, year and custom date-range fields. It also disables the
   * error wrapper, progress indicator and report generation button.</p>
   *
   * <p>This is typically used while a report is being generated or while
   * another operation requiring temporary UI locking is in progress.</p>
   */
  private void disableFields() {
    spReportType.setEnabled(false);
    spReportType.setEnabled(false);
    spMonth.setEnabled(false);
    spQuarter.setEnabled(false);
    dpDate.setEnabled(false);
    etYear.setEnabled(false);
    dpCustomRangeFrom.setEnabled(false);
    dpCustomRangeTo.setEnabled(false);
    btnGenerate.setEnabled(false);
  }

  /**
   * Displays a Material single-date picker and passes the selected date
   * to the supplied change handler.
   *
   * <p>When {@code timestamp} is not {@code null}, the picker opens with
   * that date preselected. After the user confirms a date, the selected
   * value is converted to a Firebase {@link Timestamp} and supplied to
   * {@code dateChangeHandler}.</p>
   *
   * @param date         the currently selected date to display initially,
   *                          or {@code null} when no date has been selected
   * @param spinnerID id of the spinner calling the method
   */
  private void showDatePicker(LocalDate date, int spinnerID) {
    MaterialDatePicker.Builder<Long> builder =
            MaterialDatePicker.Builder
                    .datePicker()
                    .setTitleText(
                            getString(R.string.reports_date_picker_text)
                    );

    /*
     * When a date was previously selected, open the picker
     * with that date already selected.
     */
    if (date != null) {
      builder.setSelection(
              date.atStartOfDay()
                      .toInstant(ZoneOffset.UTC)
                      .toEpochMilli()
      );
    }

    MaterialDatePicker<Long> datePicker =
            builder.build();

    datePicker.addOnPositiveButtonClickListener(selection -> {
      if (selection == null) {
        return;
      }


      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
      LocalDate selected = Instant.ofEpochMilli(selection)
              .atZone(ZoneOffset.UTC)
              .toLocalDate();

      if(spinnerID == R.id.dpDate) {
        dpDate.setText(formatter.format(selected));
        dpDateValue = selected;
      }
      else if (spinnerID == R.id.dpCustomRangeFrom) {
        dpCustomRangeFrom.setText(formatter.format(selected));
        dpCustomRangeFromValue = selected;
      }
      else if (spinnerID == R.id.dpCustomRangeTo) {
        dpCustomRangeTo.setText(formatter.format(selected));
        dpCustomRangeToValue = selected;
      }
    });

    datePicker.show(
            getSupportFragmentManager(),
            "reports_activity_date_picker"
    );
  }

  /**
   * Validates the selected report configuration, retrieves the matching
   * transactions and generates the report HTML asynchronously.
   *
   * <p>The method first reads the entered year, displays the progress indicator,
   * shows a report-generation message and disables the report configuration
   * controls to prevent additional user interaction while generation is in
   * progress.</p>
   *
   * <p>The report generation is performed on a background thread. The method:</p>
   *
   * <ul>
   *   <li>validates the entered report parameters;</li>
   *   <li>calculates the report's start and end dates;</li>
   *   <li>retrieves the current user's transactions for the selected period;</li>
   *   <li>creates the appropriate {@link Report} implementation;</li>
   *   <li>retrieves the user's configured home currency;</li>
   *   <li>generates the report HTML using {@link ReportHtmlService}.</li>
   * </ul>
   *
   * <p>After successful generation, the UI controls are enabled again, the
   * progress indicator is hidden and the generated HTML is loaded into the
   * report {@link WebView} on the main thread.</p>
   *
   * <p>Validation and unexpected exceptions are forwarded to
   * {@code handleException(...)} on the main thread.</p>
   */
  private void handleGenerateReport() {
    etYearValue = etYear.getText().toString().trim();

    toggleProgressBarVisibility();
    showToast(getString(R.string.report_generating_message));
    disableFields();

    Executors.newSingleThreadExecutor().execute(() -> {
      try {
        handleValidation();
        handleReportTimeRange();
        String uid = authService.getUserID();
        List<Transaction> transactionList = databaseService.getTransactions(uid, from, to);

        Report report;

        switch (reportType) {
          case DAILY:
            report = new DailyReport(dpDateValue, transactionList);
            break;
          case MONTHLY:
            report  = new MonthlyReport(spMonthValue, Integer.parseInt(etYearValue), transactionList);
            break;
          case QUARTERLY:
            report = new QuarterlyReport(spQuarterValue, Integer.parseInt(etYearValue), transactionList);
            break;
          case YEARLY:
            report = new YearlyReport(Integer.parseInt(etYearValue), transactionList);
            break;
          default:
            report = new CustomReport(dpCustomRangeFromValue, dpCustomRangeToValue, transactionList);
            break;
        }
        UserProfile userProfile = CacheService.read(CacheKey.USER_PROFILE, UserProfile.class);
        ReportHtmlService reportHtmlService = new ReportHtmlService(
                report,
                ReportsActivity.this,
                userProfile != null ? CurrencyCode.parse(userProfile.getHomeCurrency()) : ""
        );
        reportHtml = reportHtmlService.generateHtml();
        runOnUiThread(() -> {
          enableFields();
          toggleProgressBarVisibility();
          webView.loadDataWithBaseURL(
                  null,
                  reportHtml,       // Your HTML string variable
                  "text/html",        // Content type
                  "UTF-8",            // Character encoding
                  null
          );
        });
      } catch(ValidationException exception) {
        runOnUiThread(() -> {
          enableFields();
          handleException(exception, ReportsActivity.class);
        });
      } catch(Exception exception) {
        runOnUiThread(() -> handleException(exception, getString(R.string.error_general), ReportsActivity.class));
      }
    });
  }

  /**
   * Validates the input fields required for the selected report type.
   *
   * <p>For a {@link ReportType#CUSTOM} report, both the start and end dates
   * must be selected, and the start date must not be later than the end date.
   * For a {@link ReportType#DAILY} report, a report date must be selected.
   * For other report types, the year field must not be empty.</p>
   *
   * @throws ValidationException if a required value is missing or the custom
   *                             date range is invalid
   */
  private void handleValidation() {
    if(reportType == ReportType.CUSTOM) {
      if(dpCustomRangeFromValue == null) throw new ValidationException(getString(R.string.report_from_required), null);
      if(dpCustomRangeToValue == null) throw new ValidationException(getString(R.string.report_to_required), null);
      if(dpCustomRangeFromValue.isAfter(dpCustomRangeToValue)) throw new ValidationException(getString(R.string.report_from_bigger_than_to), null);
    } else if(reportType == ReportType.DAILY) {
      if(dpDateValue == null) throw new ValidationException(getString(R.string.report_date_required), null);
    } else if (etYearValue.isEmpty()) {
      throw new ValidationException(getString(R.string.report_year_required), null);
    }
  }

  /**
   * Determines and sets the report time range based on the selected {@code reportType}.
   *
   * <p>The calculated range is stored in the {@code from} and {@code to} fields:
   * <ul>
   *   <li>{@code DAILY}: uses the selected date for both boundaries.</li>
   *   <li>{@code MONTHLY}: uses the first and last day of the selected month.</li>
   *   <li>{@code QUARTERLY}: uses the first and last day of the selected quarter.</li>
   *   <li>{@code YEARLY}: uses the first and last day of the selected year.</li>
   *   <li>{@code CUSTOM}: uses the manually selected start and end dates.</li>
   * </ul>
   *
   * <p>Calendar-based dates are converted to timestamps at the start of the day
   * using the system-default time zone.
   *
   * @throws NumberFormatException if the provided year value is not a valid integer
   * @throws java.time.DateTimeException if the selected year, month, or quarter
   *         produces an invalid date
   */
  private void handleReportTimeRange() {
    int year =  !etYearValue.isEmpty() ? Integer.parseInt(etYearValue) : 0;
    LocalDate firstDay = null;
    LocalDate lastDay = null;
    switch (reportType) {
      case DAILY:
        firstDay = dpDateValue;
        lastDay = dpDateValue;
        break;
      case MONTHLY:
        // starting with the first day of the month
        firstDay = LocalDate.of(year, spMonthValue.getMonthValue(), 1);
        // getting the last day of the month
        lastDay = firstDay.with(TemporalAdjusters.lastDayOfMonth());
        break;
      case QUARTERLY:
        // starting with the first day of the year
        firstDay = LocalDate.of(year, spQuarterValue.getStartMonthNumber(), 1);
        // getting the last day of the year
        lastDay = LocalDate.of(year, spQuarterValue.getEndMonthNumber(), 1).with(TemporalAdjusters.lastDayOfMonth());
        break;
      case YEARLY:
        // starting with the first day of the year
        firstDay = LocalDate.of(year, 1, 1);
        // getting the last day of the year
        lastDay = firstDay.with(TemporalAdjusters.lastDayOfYear());
        break;
        // setting the first day of the month as the starting point of the report's time range
      case CUSTOM:
        firstDay = dpCustomRangeFromValue;
        lastDay = dpCustomRangeToValue;
        break;
    }

    // Adjusting boundary values
    ZoneId userTimeZone = ZoneId.systemDefault();

    // Beginning of the selected "from" date.
    Instant fromInstant =  firstDay
            .atStartOfDay(userTimeZone)
            .toInstant();

    // Beginning of the day after the selected final "to" date
    Instant toInstant = lastDay
            .plusDays(1)
            .atStartOfDay(userTimeZone)
            .toInstant();

    from = new Timestamp(Date.from(fromInstant));
    to = new Timestamp(Date.from(toInstant));
  }

  /**
   * Starts a print job for the HTML content currently displayed in the supplied
   * {@link WebView}.
   *
   * <p>The report is configured for A4 paper in landscape orientation, color
   * printing and no minimum page margins. The {@link PrintManager} opens the
   * Android system print dialog, where the user can select a printer or save the
   * document as a PDF.</p>
   *
   * <p>If the system print service is unavailable, the method performs no
   * action.</p>
   *
   * @param webView the WebView containing the HTML content to print
   * @param jobName the name displayed for the print job and generated document
   */
  private void triggerWebViewPrint(WebView webView, String jobName) {
    PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);

    if (printManager != null) {
      PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);
      PrintAttributes attributes = new PrintAttributes.Builder()
              .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
              .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
              .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
              .build();

      printManager.print(jobName, printAdapter, attributes);
    }
  }
}