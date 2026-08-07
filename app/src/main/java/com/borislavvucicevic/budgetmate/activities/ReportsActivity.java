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

/**
 * Activity responsible for generating financial reports in the BudgetMate
 * application.
 *
 * <p>This activity allows the user to generate reports for different time
 * periods, including daily, monthly, quarterly, yearly, and custom
 * date ranges. The appropriate report configuration fields are displayed
 * dynamically depending on the selected {@link ReportType}.</p>
 *
 * <p>After the report parameters have been validated, the activity calculates
 * the required date range and retrieves the authenticated user's transactions
 * from the database. The corresponding {@link Report} implementation is then
 * created and converted into HTML using {@link ReportHtmlService}.</p>
 *
 * <p>The generated HTML report is displayed in a {@link WebView}. Once the
 * report has finished loading, the Android printing framework is opened,
 * allowing the report to be printed or saved as a PDF document.</p>
 *
 * <p>Database and report-generation operations are executed on a background
 * thread to avoid blocking the Android UI thread. User-interface changes are
 * returned to the main thread through {@code runOnUiThread(...)}.</p>
 *
 * <p>The activity also supports changing the application language using
 * the locale selector inherited from {@link TemplateActivity}.</p>
 *
 * @see TemplateActivity
 * @see Report
 * @see DailyReport
 * @see MonthlyReport
 * @see QuarterlyReport
 * @see YearlyReport
 * @see CustomReport
 * @see ReportHtmlService
 */
public class ReportsActivity extends TemplateActivity {

  /**
   * Tag used when writing log messages associated with this activity.
   */
  public static final String REPORTS_ACTIVITY = "REPORTS_ACTIVITY";

  /**
   * Spinner used to select the type of report to generate.
   */
  private Spinner spReportType;

  /**
   * Spinner used to select a month for monthly reports.
   */
  private Spinner spMonth;

  /**
   * Spinner used to select a quarter for quarterly reports.
   */
  private Spinner spQuarter;

  /**
   * Input field used to specify the year for monthly, quarterly,
   * and yearly reports.
   */
  private EditText etYear;

  /**
   * Date-selection field used when generating a daily report.
   */
  private AppCompatTextView dpDate;

  /**
   * Date-selection field representing the beginning of a custom report range.
   */
  private AppCompatTextView dpCustomRangeFrom;

  /**
   * Date-selection field representing the end of a custom report range.
   */
  private AppCompatTextView dpCustomRangeTo;

  /**
   * Button used to initiate report generation.
   */
  private Button btnGenerate;

  /**
   * WebView used to display the generated HTML report.
   */
  private WebView webView;

  /**
   * Selected date used when generating a daily report.
   */
  private LocalDate dpDateValue;

  /**
   * Selected starting date of a custom report range.
   */
  private LocalDate dpCustomRangeFromValue;

  /**
   * Selected ending date of a custom report range.
   */
  private LocalDate dpCustomRangeToValue;

  /**
   * Currently selected type of report.
   *
   * <p>The default report type is {@link ReportType#DAILY}.</p>
   */
  private ReportType reportType = ReportType.DAILY;

  /**
   * String representation of the year entered by the user.
   */
  private String etYearValue;

  /**
   * Currently selected month for monthly reports.
   *
   * <p>The default month is January.</p>
   */
  private Month spMonthValue = Month.JAN;

  /**
   * Currently selected quarter for quarterly reports.
   *
   * <p>The default value is the first quarter.</p>
   */
  private Quarter spQuarterValue = Quarter.I;

  /**
   * Beginning of the database query time range.
   */
  private Timestamp from;

  /**
   * Exclusive upper boundary of the database query time range.
   */
  private Timestamp to;

  /**
   * HTML representation of the generated report.
   */
  private String reportHtml;

  /**
   * Called when the reports activity is first created.
   *
   * <p>This method enables edge-to-edge rendering and adjusts the activity
   * layout for the on-screen keyboard using window insets.</p>
   *
   * <p>It also initializes the report-type, quarter, and month selection
   * spinners.</p>
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
            findViewById(R.id.reportsMain),
            (v, insets) -> {
              int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;

              v.setPadding(
                      v.getPaddingLeft(),
                      v.getPaddingTop(),
                      v.getPaddingRight(),
                      imeBottom
              );

              return insets;
            }
    );

    // Set up report configuration spinners.
    this.setSpReportType();
    this.setSpQuarter();
    this.setSpMonth();
  }

  /**
   * Returns the layout resource used by this activity.
   *
   * @return resource identifier of the reports activity layout
   */
  @Override
  protected int getLayoutID() {
    return R.layout.activity_reports;
  }

  /**
   * Retrieves and stores references to the user-interface widgets
   * contained in the reports activity layout.
   *
   * <p>This includes report configuration spinners, date fields, year
   * input, report generation controls, progress and error elements,
   * locale selector, and the {@link WebView} used to display reports.</p>
   */
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

  /**
   * Registers event listeners for the report configuration controls.
   *
   * <p>The report-type listener updates the currently selected
   * {@link ReportType} and displays the fields required for that type.
   * Month and quarter listeners store their corresponding selections.</p>
   *
   * <p>Date fields open a {@link MaterialDatePicker}, while the report
   * generation button initiates the report-generation process.</p>
   *
   * <p>The {@link WebViewClient} waits until the generated report has
   * completely loaded before retrieving and sanitizing the document title
   * and starting the Android print process.</p>
   */
  @Override
  protected void setListeners() {
    spReportType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

              @Override
              public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ReportTypeOption reportTypeOption =
                        (ReportTypeOption) parent.getItemAtPosition(position);

                reportType = reportTypeOption.getType();

                displayFields();
              }

              @Override
              public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing.
              }
            });
    spMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

              @Override
              public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                MonthOption monthOption =
                        (MonthOption) parent.getItemAtPosition(position);

                spMonthValue = monthOption.getMonth();
              }

              @Override
              public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing.
              }
            });
    spQuarter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

              @Override
              public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                QuarterOption quarterOption =
                        (QuarterOption) parent.getItemAtPosition(position);

                spQuarterValue = quarterOption.getQuarter();
              }

              @Override
              public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing.
              }
            });
    localeSwitch.setOnItemSelectedListener(LocalisationService.getLocaleChangeHandler());
    dpDate.setOnClickListener(v -> showDatePicker(dpDateValue, v.getId()));
    dpCustomRangeFrom.setOnClickListener(v -> showDatePicker(dpCustomRangeFromValue, v.getId()));
    dpCustomRangeTo.setOnClickListener(v -> showDatePicker(dpCustomRangeToValue, v.getId()));
    btnGenerate.setOnClickListener(v -> handleGenerateReport());
    webView.setWebViewClient(
            new WebViewClient() {

              /**
               * Called when the generated report has finished loading
               * inside the WebView.
               *
               * <p>The method retrieves the HTML document title and uses
               * a generic localized title if no document title exists.
               * Characters that could cause problems in generated file
               * names are replaced before the print process is started.</p>
               *
               * @param view WebView that finished loading the report
               * @param url  URL associated with the loaded document
               */
              @Override
              public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                String documentTitle = view.getTitle();

                if (documentTitle == null || documentTitle.trim().isEmpty()) {
                  documentTitle = getString(R.string.report_generic_document_name);
                }

                // Remove characters unsuitable for file names.
                documentTitle = documentTitle.replaceAll("[\\\\/:*?\"<>|]", "_");

                triggerWebViewPrint(view, documentTitle);
              }
            }
    );
  }

  /**
   * Initializes the report-type spinner with all supported report types.
   *
   * <p>Each localized label is represented by a
   * {@link ReportTypeOption} containing the corresponding
   * {@link ReportType} value.</p>
   *
   * <p>The supported report types are:</p>
   *
   * <ul>
   *     <li>{@link ReportType#DAILY}</li>
   *     <li>{@link ReportType#MONTHLY}</li>
   *     <li>{@link ReportType#QUARTERLY}</li>
   *     <li>{@link ReportType#YEARLY}</li>
   *     <li>{@link ReportType#CUSTOM}</li>
   * </ul>
   */
  private void setSpReportType() {
    List<ReportTypeOption> reportTypeOptions = new ArrayList<>();

    reportTypeOptions.add(new ReportTypeOption(
                    getString(R.string.report_daily),
                    ReportType.DAILY
    ));

    reportTypeOptions.add(new ReportTypeOption(
                    getString(R.string.report_monthly),
                    ReportType.MONTHLY
    ));

    reportTypeOptions.add(new ReportTypeOption(
                    getString(R.string.report_quarterly),
                    ReportType.QUARTERLY
    ));

    reportTypeOptions.add(new ReportTypeOption(
                    getString(R.string.report_yearly),
                    ReportType.YEARLY
    ));

    reportTypeOptions.add(new ReportTypeOption(
                    getString(R.string.report_custom),
                    ReportType.CUSTOM
    ));

    ArrayAdapter<ReportTypeOption> adapter = new ArrayAdapter<>(
            this,
            R.layout.spinner_layout,
            reportTypeOptions
    );

    adapter.setDropDownViewResource(R.layout.spinner_layout);
    spReportType.setAdapter(adapter);
  }

  /**
   * Initializes the quarter spinner with the four quarters of a year.
   *
   * <p>Each localized label is represented by a {@link QuarterOption}
   * associated with its corresponding {@link Quarter} enumeration value.</p>
   */
  private void setSpQuarter() {
    List<QuarterOption> quarterOptions = new ArrayList<>();

    quarterOptions.add(
            new QuarterOption(
                    getString(R.string.reports_quarter_first),
                    Quarter.I
            )
    );

    quarterOptions.add(
            new QuarterOption(
                    getString(R.string.report_quarter_second),
                    Quarter.II
            )
    );

    quarterOptions.add(
            new QuarterOption(
                    getString(R.string.report_quarter_third),
                    Quarter.III
            )
    );

    quarterOptions.add(
            new QuarterOption(
                    getString(R.string.report_quarter_fourth),
                    Quarter.IV
            )
    );

    ArrayAdapter<QuarterOption> adapter = new ArrayAdapter<>(
            this,
            R.layout.spinner_layout,
            quarterOptions
    );

    adapter.setDropDownViewResource(
            R.layout.spinner_layout
    );

    spQuarter.setAdapter(adapter);
  }

  /**
   * Initializes the month spinner with all twelve months of the year.
   *
   * <p>Each localized month label is represented by a
   * {@link MonthOption} associated with its corresponding
   * {@link Month} enumeration value.</p>
   */
  private void setSpMonth() {
    List<MonthOption> monthOptions = new ArrayList<>();

    for (Month month : Month.values()) {
      int stringResId;

      switch (month) {
        case JAN:
          stringResId = R.string.jan;
          break;
        case FEB:
          stringResId = R.string.feb;
          break;
        case MAR:
          stringResId = R.string.mar;
          break;
        case APR:
          stringResId = R.string.apr;
          break;
        case MAY:
          stringResId = R.string.may;
          break;
        case JUN:
          stringResId = R.string.jun;
          break;
        case JUL:
          stringResId = R.string.jul;
          break;
        case AUG:
          stringResId = R.string.aug;
          break;
        case SEP:
          stringResId = R.string.sep;
          break;
        case OCT:
          stringResId = R.string.oct;
          break;
        case NOV:
          stringResId = R.string.nov;
          break;
        case DEC:
          stringResId = R.string.dec;
          break;
        default:
          throw new IllegalArgumentException("Unknown month: " + month);
      }

      monthOptions.add(new MonthOption(getString(stringResId), month));
    }

    ArrayAdapter<MonthOption> adapter = new ArrayAdapter<>(
            this,
            R.layout.spinner_layout,
            monthOptions
    );

    adapter.setDropDownViewResource(R.layout.spinner_layout);
    spMonth.setAdapter(adapter);
  }

  /**
   * Displays the input fields required by the currently selected report type.
   *
   * <p>All report configuration fields are initially hidden by
   * {@link #hideFields()}. The method then displays only the controls
   * required for the value stored in {@link #reportType}.</p>
   *
   * <ul>
   *     <li>{@link ReportType#DAILY} displays the date selector.</li>
   *     <li>{@link ReportType#MONTHLY} displays the month and year fields.</li>
   *     <li>{@link ReportType#QUARTERLY} displays the quarter and year fields.</li>
   *     <li>{@link ReportType#YEARLY} displays the year field.</li>
   *     <li>{@link ReportType#CUSTOM} displays the custom start and end dates.</li>
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
        Log.d(
                REPORTS_ACTIVITY,
                "For some reason no field has been displayed."
        );
    }
  }

  /**
   * Hides all fields used to configure the report period.
   *
   * <p>This method is called before displaying the controls corresponding
   * to the currently selected report type.</p>
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
   * Enables the report configuration controls.
   *
   * <p>This method restores user interaction after report generation or
   * validation processing has completed.</p>
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
   * Disables the report configuration controls.
   *
   * <p>The controls are temporarily disabled while a report is being
   * generated to prevent the user from changing report parameters or
   * starting multiple report-generation operations simultaneously.</p>
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
   * Displays a Material Design date picker for one of the report date fields.
   *
   * <p>If a date has previously been selected, that date is initially
   * selected when the picker opens. The selected date is formatted as
   * {@code dd.MM.yyyy} and displayed in the corresponding field.</p>
   *
   * <p>The ID supplied through {@code spinnerID} determines which stored
   * date value and UI element should be updated.</p>
   *
   * @param date      currently selected date, or {@code null} if no date
   *                  has previously been selected
   * @param spinnerID resource ID of the date field that opened the picker
   */
  private void showDatePicker(LocalDate date, int spinnerID) {
    MaterialDatePicker.Builder<Long> builder =
            MaterialDatePicker.Builder
                    .datePicker()
                    .setTitleText(
                            getString(
                                    R.string.reports_date_picker_text
                            )
                    );

    /*
     * If a date has already been selected, display it as the
     * initially selected date.
     */
    if (date != null) {
      builder.setSelection(date.atStartOfDay()
                      .toInstant(ZoneOffset.UTC)
                      .toEpochMilli());
    }

    MaterialDatePicker<Long> datePicker = builder.build();

    datePicker.addOnPositiveButtonClickListener(
            selection -> {

              if (selection == null) {
                return;
              }

              DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

              LocalDate selected = Instant
                      .ofEpochMilli(selection)
                      .atZone(ZoneOffset.UTC)
                      .toLocalDate();

              if (spinnerID == R.id.dpDate) {
                dpDate.setText(formatter.format(selected));
                dpDateValue = selected;
              }
              else if (spinnerID == R.id.dpCustomRangeFrom) {
                dpCustomRangeFrom.setText(formatter.format(selected));
                dpCustomRangeFromValue = selected;
              }
              else if (spinnerID== R.id.dpCustomRangeTo) {
                dpCustomRangeTo.setText(formatter.format(selected));
                dpCustomRangeToValue = selected;
              }
            }
    );

    datePicker.show(
            getSupportFragmentManager(),
            "reports_activity_date_picker"
    );
  }

  /**
   * Generates a report using the currently selected report configuration.
   *
   * <p>The method retrieves the entered year, displays a progress indicator,
   * informs the user that report generation has started, and disables the
   * configuration controls.</p>
   *
   * <p>Report processing is performed using a background executor. The
   * operation performs the following steps:</p>
   *
   * <ol>
   *     <li>validates the selected report parameters;</li>
   *     <li>calculates the report start and end timestamps;</li>
   *     <li>retrieves the authenticated user's transactions;</li>
   *     <li>creates the appropriate {@link Report} implementation;</li>
   *     <li>retrieves the user's home currency from the cache;</li>
   *     <li>generates the report HTML using {@link ReportHtmlService}.</li>
   * </ol>
   *
   * <p>After successful generation, the controls are enabled again,
   * the progress indicator is hidden, and the generated HTML is loaded
   * into the activity's {@link WebView}.</p>
   *
   * <p>Validation and unexpected exceptions are handled on the Android
   * UI thread.</p>
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

        List<Transaction> transactionList =
                databaseService.getTransactions(
                        uid,
                        from,
                        to
                );

        Report report;

        switch (reportType) {

          case DAILY:
            report = new DailyReport(
                    dpDateValue,
                    transactionList
            );
            break;

          case MONTHLY:
            report = new MonthlyReport(
                    spMonthValue,
                    Integer.parseInt(etYearValue),
                    transactionList
            );
            break;

          case QUARTERLY:
            report = new QuarterlyReport(
                    spQuarterValue,
                    Integer.parseInt(etYearValue),
                    transactionList
            );
            break;

          case YEARLY:
            report = new YearlyReport(
                    Integer.parseInt(etYearValue),
                    transactionList
            );
            break;

          default:
            report = new CustomReport(
                    dpCustomRangeFromValue,
                    dpCustomRangeToValue,
                    transactionList
            );
            break;
        }

        UserProfile userProfile =
                CacheService.read(
                        CacheKey.USER_PROFILE,
                        UserProfile.class
                );

        ReportHtmlService reportHtmlService =
                new ReportHtmlService(
                        report,
                        ReportsActivity.this,
                        userProfile != null
                                ? CurrencyCode.parse(
                                userProfile.getHomeCurrency()
                        )
                                : ""
                );

        reportHtml = reportHtmlService.generateHtml();

        runOnUiThread(() -> {
          enableFields();
          toggleProgressBarVisibility();
          webView.loadDataWithBaseURL(
                  null,
                  reportHtml,
                  "text/html",
                  "UTF-8",
                  null
          );
        });

      }
      catch (ValidationException exception) {
        runOnUiThread(() -> {
          enableFields();
          this.handleException(
                  exception,
                  ReportsActivity.class
          );
        });
      }
      catch (Exception exception) {
        runOnUiThread(
                () -> this.handleException(
                        exception,
                        getString(R.string.error_general),
                        ReportsActivity.class
                )
        );
      }
    });
  }

  /**
   * Validates the fields required by the currently selected report type.
   *
   * <p>The validation requirements depend on the selected
   * {@link ReportType}:</p>
   *
   * <ul>
   *     <li>
   *         {@link ReportType#DAILY} requires a selected report date.
   *     </li>
   *     <li>
   *         {@link ReportType#CUSTOM} requires both start and end dates,
   *         and the starting date must not occur after the ending date.
   *     </li>
   *     <li>
   *         Monthly, quarterly, and yearly reports require a year value.
   *     </li>
   * </ul>
   *
   * @throws ValidationException if one of the required report parameters
   *                             is missing or a custom date range is invalid
   */
  private void handleValidation() {
    if (reportType == ReportType.CUSTOM) {

      if (dpCustomRangeFromValue == null) {
        throw new ValidationException(
                getString(
                        R.string.report_from_required
                ),
                null
        );
      }

      if (dpCustomRangeToValue == null) {
        throw new ValidationException(
                getString(
                        R.string.report_to_required
                ),
                null
        );
      }

      if (
              dpCustomRangeFromValue.isAfter(
                      dpCustomRangeToValue
              )
      ) {
        throw new ValidationException(
                getString(
                        R.string.report_from_bigger_than_to
                ),
                null
        );
      }

    } else if (
            reportType == ReportType.DAILY
    ) {

      if (dpDateValue == null) {
        throw new ValidationException(
                getString(
                        R.string.report_date_required
                ),
                null
        );
      }

    } else if (etYearValue.isEmpty()) {

      throw new ValidationException(
              getString(
                      R.string.report_year_required
              ),
              null
      );
    }
  }

  /**
   * Calculates the start and end timestamps for the selected report period.
   *
   * <p>The calculated values are stored in the {@link #from} and
   * {@link #to} fields and are later used when querying transactions
   * from the database.</p>
   *
   * <p>The time range is calculated according to the report type:</p>
   *
   * <ul>
   *     <li>
   *         {@link ReportType#DAILY}: selected date only.
   *     </li>
   *     <li>
   *         {@link ReportType#MONTHLY}: first through last day of
   *         the selected month.
   *     </li>
   *     <li>
   *         {@link ReportType#QUARTERLY}: first through last day
   *         of the selected quarter.
   *     </li>
   *     <li>
   *         {@link ReportType#YEARLY}: first through last day
   *         of the selected year.
   *     </li>
   *     <li>
   *         {@link ReportType#CUSTOM}: manually selected start
   *         and end dates.
   *     </li>
   * </ul>
   *
   * <p>The lower boundary represents the beginning of the selected
   * starting date. The upper boundary represents the beginning of the
   * day immediately following the selected ending date. This allows the
   * database query to include the entire final day of the report period.</p>
   *
   * <p>Date boundaries are converted using the system's default
   * {@link ZoneId} before being converted to Firebase
   * {@link Timestamp} values.</p>
   *
   * @throws NumberFormatException if the entered year cannot be converted
   *                               to an integer
   * @throws java.time.DateTimeException if a calculated calendar date
   *                                     is invalid
   */
  private void handleReportTimeRange() {
    int year = !etYearValue.isEmpty()
                    ? Integer.parseInt(etYearValue)
                    : 0;

    LocalDate firstDay = null;
    LocalDate lastDay = null;

    switch (reportType) {

      case DAILY:
        firstDay = dpDateValue;
        lastDay = dpDateValue;
        break;

      case MONTHLY:
        // First day of the selected month.
        firstDay = LocalDate.of(
                year,
                spMonthValue.getMonthValue(),
                1
        );

        // Last day of the selected month.
        lastDay = firstDay.with(
                TemporalAdjusters.lastDayOfMonth()
        );

        break;

      case QUARTERLY:
        // First day of the selected quarter.
        firstDay = LocalDate.of(
                year,
                spQuarterValue.getStartMonthNumber(),
                1
        );

        // Last day of the selected quarter.
        lastDay = LocalDate.of(
                        year,
                        spQuarterValue.getEndMonthNumber(),
                        1
                )
                .with(
                        TemporalAdjusters.lastDayOfMonth()
                );

        break;

      case YEARLY:
        // First day of the selected year.
        firstDay = LocalDate.of(
                year,
                1,
                1
        );

        // Last day of the selected year.
        lastDay = firstDay.with(
                TemporalAdjusters.lastDayOfYear()
        );

        break;

      case CUSTOM:
        firstDay = dpCustomRangeFromValue;
        lastDay = dpCustomRangeToValue;
        break;
    }

    ZoneId userTimeZone =
            ZoneId.systemDefault();

    /*
     * Beginning of the first selected day.
     */
    Instant fromInstant =
            firstDay
                    .atStartOfDay(userTimeZone)
                    .toInstant();

    /*
     * Beginning of the day after the final selected date.
     * This forms an exclusive upper boundary.
     */
    Instant toInstant =
            lastDay
                    .plusDays(1)
                    .atStartOfDay(userTimeZone)
                    .toInstant();

    from = new Timestamp(
            Date.from(fromInstant)
    );

    to = new Timestamp(
            Date.from(toInstant)
    );
  }

  /**
   * Starts an Android print job for the HTML report displayed in the
   * supplied {@link WebView}.
   *
   * <p>The method obtains the system {@link PrintManager} and creates
   * a {@link PrintDocumentAdapter} from the WebView. The report is
   * configured for A4 landscape paper, color output, and no minimum
   * margins.</p>
   *
   * <p>The Android print dialog allows the user to send the report to
   * an available printer or save it as a PDF document.</p>
   *
   * <p>If the system print service cannot be obtained, no print operation
   * is performed.</p>
   *
   * @param webView WebView containing the generated report to print
   * @param jobName name used for the print job and generated document
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