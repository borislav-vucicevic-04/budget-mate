package com.borislavvucicevic.budgetmate.services;

import android.content.Context;

import androidx.annotation.NonNull;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.enums.CacheKey;
import com.borislavvucicevic.budgetmate.enums.Month;
import com.borislavvucicevic.budgetmate.enums.Quarter;
import com.borislavvucicevic.budgetmate.enums.ReportType;
import com.borislavvucicevic.budgetmate.enums.TransactionType;
import com.borislavvucicevic.budgetmate.models.AveragedReport;
import com.borislavvucicevic.budgetmate.models.Category;
import com.borislavvucicevic.budgetmate.models.MonthlyReport;
import com.borislavvucicevic.budgetmate.models.QuarterlyReport;
import com.borislavvucicevic.budgetmate.models.Report;
import com.borislavvucicevic.budgetmate.models.Transaction;
import com.borislavvucicevic.budgetmate.models.YearlyReport;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Generates a localized HTML representation of a financial report.
 *
 * <p>The service uses HTML templates stored in the application's raw resources.
 * Template placeholders are replaced with information obtained from the supplied
 * {@link Report}, such as totals, category summaries, monthly totals, averages,
 * reporting periods and transactions.</p>
 *
 * <p>The generated content depends on the report's {@link ReportType}. Certain
 * sections, such as monthly totals and averages, are omitted for daily reports.
 * Report-specific titles and period information are generated for monthly,
 * quarterly, yearly and custom reports.</p>
 *
 * <p>All user-facing labels are loaded through the supplied Android
 * {@link Context}, allowing the resulting HTML report to use the application's
 * localized string resources.</p>
 */
public class ReportHtmlService {

  /**
   * Report whose data is used to generate the HTML document.
   */
  private final Report report;

  /**
   * Android context used to access localized strings and raw HTML templates.
   */
  private final Context context;

  /**
   * Currency symbol or currency code appended to formatted amounts.
   */
  private final String currency;

  /**
   * Creates a service for generating an HTML representation of a report.
   *
   * @param report   report containing the financial data to render
   * @param context  Android context used to access resources and localized strings
   * @param currency currency symbol or code displayed next to monetary values,
   *                 such as {@code "€"}, {@code "$"} or {@code "EUR"}
   */
  public ReportHtmlService(Report report, Context context, String currency) {
    this.report = report;
    this.context = context;
    this.currency = currency;
  }

  /**
   * Generates the complete HTML document for the configured report.
   *
   * <p>The method loads the main report template and generates the appropriate
   * sections based on the report type. It then replaces all template placeholders
   * with localized labels and formatted report data.</p>
   *
   * <p>The generated document may contain:</p>
   *
   * <ul>
   *   <li>a report title;</li>
   *   <li>a reporting-period summary;</li>
   *   <li>income and expense totals;</li>
   *   <li>totals grouped by category;</li>
   *   <li>totals grouped by month;</li>
   *   <li>daily and monthly averages;</li>
   *   <li>a list of transactions.</li>
   * </ul>
   *
   * @return complete HTML representation of the report
   */
  public String generateHtml() {
    String template = loadTemplate(R.raw.report_template);
    String reportTitle = "";
    String reportPeriodSection = "";
    String totalsSection = generateTotalsSection();
    String totalsByCategorySection = generateTotalsByCategorySection();
    String totalsByMonthSection =
            report.getType() != ReportType.DAILY
                    ? generateTotalsByMonthSection()
                    : "";
    String averagesSection =
            report.getType() != ReportType.DAILY
                    ? generateAveragesSection()
                    : "";
    String transactionsSection = generateTransactionsSection();

    switch (report.getType()) {
      case DAILY:
        reportTitle = context.getString(R.string.daily_report_title)
                .replace("[date]", formatLocalDateAsString(report.getFrom()));
        averagesSection = "";
        totalsByMonthSection = "";
        break;

      case MONTHLY:
        reportTitle = context.getString(R.string.monthly_report_title)
                .replace(
                        "[month]",
                        formatMonthAsString(((MonthlyReport) report).getMonth())
                )
                .replace(
                        "[year]",
                        ((MonthlyReport) report).getYear().toString()
                );
        break;

      case QUARTERLY:
        reportTitle = context.getString(R.string.quarterly_report_title)
                .replace(
                        "[quarter]",
                        formatQuarterAsString(
                                ((QuarterlyReport) report).getQuarter()
                        )
                )
                .replace(
                        "[year]",
                        ((QuarterlyReport) report).getYear().toString()
                );
        reportPeriodSection = generateReportPeriodSection();
        break;

      case YEARLY:
        reportTitle = context.getString(R.string.yearly_report_title)
                .replace(
                        "[year]",
                        ((YearlyReport) report).getYear().toString()
                );
        break;

      case CUSTOM:
        reportTitle = context.getString(R.string.custom_report_title);
        reportPeriodSection = generateReportPeriodSection();
    }

    return template.replace("[report title]", reportTitle)
            .replace("[report period section]", reportPeriodSection)
            .replace("[totals section]", totalsSection)
            .replace(
                    "[totals by category section]",
                    totalsByCategorySection
            )
            .replace("[totals by month section]", totalsByMonthSection)
            .replace("[averages section]", averagesSection)
            .replace("[transactions section]", transactionsSection)
            .replace(
                    "[income text]",
                    context.getString(R.string.income)
            )
            .replace(
                    "[expense text]",
                    context.getString(R.string.expense)
            );
  }

  /**
   * Formats a monetary amount for display in the generated report.
   *
   * <p>The absolute value of the supplied amount is used. Income values receive
   * a plus sign, while expense values receive a minus sign. A {@code null}
   * amount is treated as zero.</p>
   *
   * <p>Amounts are formatted with two decimal places using {@link Locale#US}.</p>
   *
   * @param amount          amount to format; may be {@code null}
   * @param transactionType transaction type that determines whether a positive
   *                        or negative sign is displayed
   * @return formatted amount containing its sign, value and currency
   */
  @NonNull
  private String formatAmountAsString(
          Double amount,
          TransactionType transactionType
  ) {
    double value = amount == null ? 0.0 : Math.abs(amount);

    if (transactionType == TransactionType.EXPENSE) {
      return String.format(Locale.US, "-%.2f %s", value, currency);
    }

    return String.format(Locale.US, "+%.2f %s", value, currency);
  }

  /**
   * Formats a date using the {@code dd.MM.yyyy} pattern.
   *
   * @param localDate date to format
   * @return formatted date, for example {@code "04.08.2026"}
   */
  private String formatLocalDateAsString(LocalDate localDate) {
    DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    return formatter.format(localDate);
  }

  /**
   * Returns the localized display name of a month.
   *
   * <p>The month name is obtained from the application's string resources.</p>
   *
   * @param month month whose localized name should be returned
   * @return localized month name, or an empty string when no matching month
   *         can be determined
   */
  private String formatMonthAsString(@NonNull Month month) {
    String monthName;

    switch (month) {
      case JAN:
        monthName = context.getString(R.string.jan);
        break;
      case FEB:
        monthName = context.getString(R.string.feb);
        break;
      case MAR:
        monthName = context.getString(R.string.mar);
        break;
      case APR:
        monthName = context.getString(R.string.apr);
        break;
      case MAY:
        monthName = context.getString(R.string.may);
        break;
      case JUN:
        monthName = context.getString(R.string.jun);
        break;
      case JUL:
        monthName = context.getString(R.string.jul);
        break;
      case AUG:
        monthName = context.getString(R.string.aug);
        break;
      case SEP:
        monthName = context.getString(R.string.sep);
        break;
      case OCT:
        monthName = context.getString(R.string.oct);
        break;
      case NOV:
        monthName = context.getString(R.string.nov);
        break;
      case DEC:
        monthName = context.getString(R.string.dec);
        break;
      default:
        monthName = "";
    }

    return monthName;
  }

  /**
   * Returns the localized display name of a quarter.
   *
   * <p>The returned value is converted to lowercase before it is inserted into
   * the report title.</p>
   *
   * @param quarter quarter whose localized name should be returned
   * @return localized lowercase quarter name, or an empty string when no
   *         matching quarter can be determined
   */
  @NonNull
  private String formatQuarterAsString(@NonNull Quarter quarter) {
    switch (quarter) {
      case I:
        return context.getString(
                R.string.reports_quarter_first
        ).toLowerCase();

      case II:
        return context.getString(
                R.string.report_quarter_second
        ).toLowerCase();

      case III:
        return context.getString(
                R.string.report_quarter_third
        ).toLowerCase();

      case IV:
        return context.getString(
                R.string.report_quarter_fourth
        ).toLowerCase();
    }

    return "";
  }

  /**
   * Loads a UTF-8 encoded template from the application's raw resources.
   *
   * @param resourceID identifier of the raw resource containing the template
   * @return complete template contents, or an empty string when the resource
   *         contains no text
   */
  private String loadTemplate(int resourceID) {
    InputStream inputStream = context.getResources()
            .openRawResource(resourceID);

    Scanner scanner = new Scanner(
            inputStream,
            StandardCharsets.UTF_8.name()
    ).useDelimiter("\\A");

    String template = scanner.hasNext() ? scanner.next() : "";

    scanner.close();

    return template;
  }

  /**
   * Generates the HTML section describing the report period.
   *
   * <p>The section includes the start date, end date, number of days and number
   * of months covered by the report. Dates are formatted using the
   * {@code dd.MM.yyyy} pattern.</p>
   *
   * <p>This method expects the configured report to implement
   * {@link AveragedReport}.</p>
   *
   * @return rendered report-period HTML section
   */
  @NonNull
  private String generateReportPeriodSection() {
    DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    String template = loadTemplate(R.raw.report_period);

    return template.replace(
                    "[report period title]",
                    context.getString(
                            R.string.report_period_section_title
                    )
            )
            .replace(
                    "[from text]",
                    context.getString(R.string.report_period_from)
            )
            .replace(
                    "[from date]",
                    formatter.format(report.getFrom())
            )
            .replace(
                    "[to text]",
                    context.getString(R.string.report_period_to)
            )
            .replace(
                    "[to date]",
                    formatter.format(report.getTo())
            )
            .replace(
                    "[day count text]",
                    context.getString(R.string.report_day_count)
            )
            .replace(
                    "[day count]",
                    String.valueOf(
                            ((AveragedReport) report).getDayCount()
                    )
            )
            .replace(
                    "[month count text]",
                    context.getString(R.string.report_month_count)
            )
            .replace(
                    "[month count]",
                    String.valueOf(
                            ((AveragedReport) report).getMonthCount()
                    )
            );
  }

  /**
   * Generates the HTML section containing total income and total expenses.
   *
   * <p>Missing totals are treated as zero by
   * {@link #formatAmountAsString(Double, TransactionType)}.</p>
   *
   * @return rendered totals HTML section
   */
  @NonNull
  private String generateTotalsSection() {
    Map<TransactionType, Double> totals = report.getTotals();
    String template = loadTemplate(R.raw.totals);

    Double income = totals.get(TransactionType.INCOME);
    Double expense = totals.get(TransactionType.EXPENSE);

    return template.replace(
                    "[income amount]",
                    formatAmountAsString(
                            income,
                            TransactionType.INCOME
                    )
            )
            .replace(
                    "[expense amount]",
                    formatAmountAsString(
                            expense,
                            TransactionType.EXPENSE
                    )
            )
            .replace(
                    "[totals title]",
                    context.getString(
                            R.string.report_totals_section_title
                    )
            );
  }

  /**
   * Generates the HTML section containing income and expense totals grouped
   * by category.
   *
   * <p>Income and expense categories are sorted by their values using
   * {@link MapSorterService}. The two lists are displayed side by side. When
   * one list contains fewer entries than the other, the missing cells are
   * rendered as empty strings.</p>
   *
   * @return rendered category-totals HTML section
   */
  @NonNull
  private String generateTotalsByCategorySection() {
    Map<String, Double> sortedIncomes =
            MapSorterService.sortByValue(
                    report.getTotalIncomesByCategory()
            );

    Map<String, Double> sortedExpenses =
            MapSorterService.sortByValue(
                    report.getTotalExpensesByCategory()
            );

    List<Map.Entry<String, Double>> totalIncomes =
            new ArrayList<>(sortedIncomes.entrySet());

    List<Map.Entry<String, Double>> totalExpenses =
            new ArrayList<>(sortedExpenses.entrySet());

    String template =
            loadTemplate(R.raw.totals_by_category);

    String rowTemplate =
            loadTemplate(R.raw.totals_by_category_row);

    StringBuilder rowsBuilder = new StringBuilder();

    int rowCount = Math.max(
            totalIncomes.size(),
            totalExpenses.size()
    );

    for (int i = 0; i < rowCount; i++) {
      Map.Entry<String, Double> income =
              i < totalIncomes.size()
                      ? totalIncomes.get(i)
                      : null;

      Map.Entry<String, Double> expense =
              i < totalExpenses.size()
                      ? totalExpenses.get(i)
                      : null;

      rowsBuilder.append(
              rowTemplate.replace(
                              "[income category name]",
                              income != null
                                      ? income.getKey()
                                      : ""
                      )
                      .replace(
                              "[income amount]",
                              income != null
                                      ? formatAmountAsString(
                                      income.getValue(),
                                      TransactionType.INCOME
                              )
                                      : ""
                      )
                      .replace(
                              "[expense category name]",
                              expense != null
                                      ? expense.getKey()
                                      : ""
                      )
                      .replace(
                              "[expense amount]",
                              expense != null
                                      ? formatAmountAsString(
                                      expense.getValue(),
                                      TransactionType.EXPENSE
                              )
                                      : ""
                      )
      );
    }

    return template.replace(
                    "[totals by category title]",
                    context.getString(
                            R.string
                                    .report_totals_by_category_section_title
                    )
            )
            .replace(
                    "[rows]",
                    rowsBuilder.toString()
            );
  }

  /**
   * Generates the HTML section containing income and expense totals grouped
   * by month.
   *
   * <p>The method iterates from the month containing the report's start date
   * through the month containing its end date. Missing monthly values are
   * formatted as zero.</p>
   *
   * <p>This method expects the configured report to implement
   * {@link AveragedReport}.</p>
   *
   * @return rendered monthly-totals HTML section
   */
  @NonNull
  private String generateTotalsByMonthSection() {
    Map<Month, Double> totalIncomes =
            ((AveragedReport) report).getTotalIncomesByMonth();

    Map<Month, Double> totalExpenses =
            ((AveragedReport) report).getTotalExpensesByMonth();

    int startMonth = report.getFrom().getMonthValue();
    int endMonth = report.getTo().getMonthValue();

    String template =
            loadTemplate(R.raw.totals_by_month);

    String rowTemplate =
            loadTemplate(R.raw.totals_by_month_row);

    StringBuilder rowsBuilder = new StringBuilder();

    for (int i = startMonth; i <= endMonth; i++) {
      Month month = Month.parseMonth(i);

      Double income = totalIncomes.get(month);
      Double expense = totalExpenses.get(month);

      String monthName = formatMonthAsString(month);

      rowsBuilder.append(
              rowTemplate.replace(
                              "[month]",
                              monthName
                      )
                      .replace(
                              "[income amount]",
                              formatAmountAsString(
                                      income,
                                      TransactionType.INCOME
                              )
                      )
                      .replace(
                              "[expense amount]",
                              formatAmountAsString(
                                      expense,
                                      TransactionType.EXPENSE
                              )
                      )
      );
    }

    return template.replace(
                    "[totals by month title]",
                    context.getString(
                            R.string
                                    .report_totals_by_month_section_title
                    )
            )
            .replace(
                    "[rows]",
                    rowsBuilder.toString()
            );
  }

  /**
   * Generates the HTML section containing average income and expenses per day
   * and per month.
   *
   * <p>This method expects the configured report to implement
   * {@link AveragedReport}. Missing average values are formatted as zero.</p>
   *
   * @return rendered averages HTML section
   */
  @NonNull
  private String generateAveragesSection() {
    Map<TransactionType, Double> averagesPerDay =
            ((AveragedReport) report).getAveragesPerDay();

    Map<TransactionType, Double> averagesPerMonth =
            ((AveragedReport) report).getAveragesPerMonth();

    Double incomePerDay =
            averagesPerDay.get(TransactionType.INCOME);

    Double incomePerMonth =
            averagesPerMonth.get(TransactionType.INCOME);

    Double expensePerDay =
            averagesPerDay.get(TransactionType.EXPENSE);

    Double expensePerMonth =
            averagesPerMonth.get(TransactionType.EXPENSE);

    String template = loadTemplate(R.raw.averages);

    return template.replace(
                    "[averages title]",
                    context.getString(
                            R.string.report_averages_section_title
                    )
            )
            .replace(
                    "[per day text]",
                    context.getString(
                            R.string.report_average_per_day
                    )
            )
            .replace(
                    "[per month text]",
                    context.getString(
                            R.string.report_average_per_month
                    )
            )
            .replaceFirst(
                    Pattern.quote("[income amount]"),
                    formatAmountAsString(
                            incomePerDay,
                            TransactionType.INCOME
                    )
            )
            .replaceFirst(
                    Pattern.quote("[expense amount]"),
                    formatAmountAsString(
                            expensePerDay,
                            TransactionType.EXPENSE
                    )
            )
            .replaceFirst(
                    Pattern.quote("[income amount]"),
                    formatAmountAsString(
                            incomePerMonth,
                            TransactionType.INCOME
                    )
            )
            .replaceFirst(
                    Pattern.quote("[expense amount]"),
                    formatAmountAsString(
                            expensePerMonth,
                            TransactionType.EXPENSE
                    )
            );
  }

  /**
   * Generates the HTML section containing all transactions included in the
   * report.
   *
   * <p>For each transaction, the method renders:</p>
   *
   * <ul>
   *   <li>the formatted transaction amount;</li>
   *   <li>the category name retrieved from {@link CacheService};</li>
   *   <li>the creation date;</li>
   *   <li>the optional modification date;</li>
   *   <li>optional transaction notes.</li>
   * </ul>
   *
   * <p>Transaction dates are converted using the system's default
   * {@link ZoneId} and formatted using the {@code dd.MM.yyyy} pattern.
   * Missing modification dates and notes are rendered as empty strings.</p>
   *
   * @return rendered transactions HTML section
   */
  @NonNull
  private String generateTransactionsSection() {
    DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    List<Transaction> transactionList =
            report.getTransactionList();

    String template =
            loadTemplate(R.raw.transactions);

    String rowTemplate =
            loadTemplate(R.raw.transactions_row);

    StringBuilder rowsBuilder = new StringBuilder();

    for (Transaction transaction : transactionList) {
      rowsBuilder.append(
              rowTemplate.replace(
                              "[amount value]",
                              formatAmountAsString(
                                      transaction.getAmount(),
                                      transaction.getType()
                              )
                      )
                      .replace(
                              "[category value]",
                              CacheService.get(
                                      CacheKey.CATEGORIES,
                                      transaction.getCategoryID(),
                                      Category.class
                              ).getName()
                      )
                      .replace(
                              "[created on value]",
                              formatter.format(
                                      transaction.getCreatedOn()
                                              .toInstant()
                                              .atZone(
                                                      ZoneId.systemDefault()
                                              )
                                              .toLocalDate()
                              )
                      )
                      .replace(
                              "[modified on value]",
                              transaction.getModifiedOn() == null
                                      ? ""
                                      : formatter.format(
                                      transaction.getModifiedOn()
                                      .toInstant()
                                      .atZone(
                                              ZoneId
                                              .systemDefault()
                                      )
                                      .toLocalDate()
                              )
                      )
                      .replace(
                              "[notes value]",
                              transaction.getNotes() != null
                                      ? transaction.getNotes()
                                      : ""
                      )
      );
    }

    return template.replace(
                    "[transactions title]",
                    context.getString(R.string.report_transactions_section_title)
            )
            .replace(
                    "[amount text]",
                    context.getString(R.string.amount)
            )
            .replace(
                    "[category text]",
                    context.getString(R.string.category)
            )
            .replace(
                    "[created on text]",
                    context.getString(R.string.created_on)
            )
            .replace(
                    "[modified on text]",
                    context.getString(R.string.modified_on)
            )
            .replace(
                    "[notes text]",
                    context.getString(R.string.notes)
            )
            .replace(
                    "[rows]",
                    rowsBuilder.toString()
            );
  }
}