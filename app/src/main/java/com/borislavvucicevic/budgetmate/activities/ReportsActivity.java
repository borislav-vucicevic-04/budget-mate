package com.borislavvucicevic.budgetmate.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.options.MonthOption;
import com.borislavvucicevic.budgetmate.options.QuarterOption;
import com.borislavvucicevic.budgetmate.options.ReportTypeOption;
import com.borislavvucicevic.budgetmate.enums.Month;
import com.borislavvucicevic.budgetmate.enums.Quarter;
import com.borislavvucicevic.budgetmate.enums.ReportType;

import java.util.ArrayList;
import java.util.List;

public class ReportsActivity extends AppCompatActivity {
  public static final String REPORTS_ACTIVITY = "REPORTS_ACTIVITY";
  private Spinner spReportType, spMonth, spQuarter;
  private EditText etDate, etYear, etCustomRangeFrom, etCustomRangeTo;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_reports);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.reportsMain), (v, insets) -> {
      int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
      v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), imeBottom);
      return insets;
    });

    // grabbing widgets
    this.grabWidgets();
    // setting up spinners
    this.setSpReportType();
    this.setSpQuarter();
    this.setSpMonth();
    // setting event listeners
    this.setListeners();
  }

  /**
   * Retrieves and stores references to the report-related UI components.
   *
   * <p>Each view is located in the current activity layout using
   * {@link #findViewById(int)} and assigned to its corresponding instance
   * variable for later use.</p>
   */
  private void grabWidgets() {
    spReportType = findViewById(R.id.spReportType);
    spMonth = findViewById(R.id.spMonth);
    spQuarter = findViewById(R.id.spQuarter);
    etDate = findViewById(R.id.etDate);
    etYear = findViewById(R.id.etYear);
    etCustomRangeFrom = findViewById(R.id.etCustomRangeFrom);
    etCustomRangeTo = findViewById(R.id.etCustomRangeTo);
  }

  /**
   * Configures the event listeners for the activity's user-interface components.
   *
   * <p>This method serves as the central location for registering all listeners
   * required by the activity. Currently, it configures the report-type spinner
   * to display the appropriate input fields whenever the selected report type
   * changes.</p>
   *
   * <p>Additional listeners should be registered here as the activity's
   * functionality is expanded.</p>
   */
  private void setListeners() {
    spReportType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // Cast the item directly to your custom object type
        ReportTypeOption reportTypeOption = (ReportTypeOption) parent.getItemAtPosition(position);
        displayFields(reportTypeOption.getType());
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        // DO NOTHING
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
   *
   * @param reportType the report type whose required input fields should be displayed
   */
  private void displayFields(ReportType reportType) {
    this.hideFields();
    Log.d(REPORTS_ACTIVITY, "Displaying fields for report type: " + reportType);

    switch (reportType) {
      case DAILY:
        etDate.setVisibility(View.VISIBLE);
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
        etCustomRangeFrom.setVisibility(View.VISIBLE);
        etCustomRangeTo.setVisibility(View.VISIBLE);
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
    Log.d(REPORTS_ACTIVITY, "Fields are now hidden.");
    spMonth.setVisibility(View.GONE);
    spQuarter.setVisibility(View.GONE);
    etDate.setVisibility(View.GONE);
    etYear.setVisibility(View.GONE);
    etCustomRangeFrom.setVisibility(View.GONE);
    etCustomRangeTo.setVisibility(View.GONE);
  }
}