package com.borislavvucicevic.budgetmate.models;

import android.util.Log;

import com.borislavvucicevic.budgetmate.enums.CacheKey;
import com.borislavvucicevic.budgetmate.enums.Month;
import com.borislavvucicevic.budgetmate.enums.Quarter;
import com.borislavvucicevic.budgetmate.enums.ReportType;
import com.borislavvucicevic.budgetmate.enums.TransactionType;
import com.borislavvucicevic.budgetmate.services.CacheService;
import com.google.firebase.Timestamp;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a financial report generated from a collection of transactions.
 *
 * <p>The report calculates and stores:</p>
 * <ul>
 *   <li>Total income and expenses grouped by category</li>
 *   <li>Total income and expenses grouped by month</li>
 *   <li>Overall totals for each transaction type</li>
 *   <li>Average income and expenses per day</li>
 *   <li>Average income and expenses per month</li>
 * </ul>
 *
 * <p>The report is prepared automatically when an instance is created.
 * Transaction categories are resolved through {@link CacheService}.</p>
 *
 * <p>The supplied {@code to} timestamp is treated as an exclusive boundary
 * when calculating the number of days in the reporting period.</p>
 *
 * @author Borislav Vucicevic
 */
public class Report {

  /**
   * Log tag used for report-related debug messages.
   */
  private static final String REPORT = "REPORT";

  /**
   * Type of report being generated.
   */
  private final ReportType type;

  /**
   * Inclusive starting timestamp of the reporting period.
   */
  private final Timestamp from;

  /**
   * Exclusive ending timestamp of the reporting period.
   */
  private final Timestamp to;

  /**
   * Quarter associated with the report, when applicable.
   */
  private final Quarter quarter;

  /**
   * Number of days included in the reporting period.
   */
  private long dayCount;

  /**
   * Number of calendar months touched by the reporting period.
   */
  private long monthCount;

  /**
   * Transactions included in the report.
   */
  private final List<Transaction> transactionList;

  /**
   * Total income grouped by category name.
   */
  private final Map<String, Double> totalIncomesByCategory = new HashMap<>();

  /**
   * Total expenses grouped by category name.
   */
  private final Map<String, Double> totalExpensesByCategory = new HashMap<>();

  /**
   * Total income grouped by month.
   */
  private final Map<Month, Double> totalIncomesByMonth = new HashMap<>();

  /**
   * Total expenses grouped by month.
   */
  private final Map<Month, Double> totalExpensesByMonth = new HashMap<>();

  /**
   * Overall transaction amounts grouped by transaction type.
   */
  private final Map<TransactionType, Double> totals = new HashMap<>();

  /**
   * Average transaction amounts per day, grouped by transaction type.
   */
  private final Map<TransactionType, Double> averagesPerDay = new HashMap<>();

  /**
   * Average transaction amounts per month, grouped by transaction type.
   */
  private final Map<TransactionType, Double> averagesPerMonth = new HashMap<>();

  /**
   * Creates and prepares a new financial report.
   *
   * <p>Report totals, date counts, and averages are calculated immediately
   * during construction.</p>
   *
   * @param type            type of report to generate
   * @param from            inclusive starting timestamp of the report
   * @param to              exclusive ending timestamp of the report
   * @param quarter         quarter associated with the report, or {@code null}
   *                        if the report is not quarter-based
   * @param transactionList transactions to include in the report
   * @throws NullPointerException if a required argument is {@code null} and is
   *                              accessed during report preparation
   */
  public Report(ReportType type, Timestamp from, Timestamp to, Quarter quarter,List<Transaction> transactionList) {
    this.type = type;
    this.from = from;
    this.to = to;
    this.quarter = quarter;
    this.dayCount = 0;
    this.monthCount = 0;
    this.transactionList = transactionList;

    prepare();
  }

  /**
   * Returns the type of this report.
   *
   * @return the report type
   */
  public ReportType getType() {
    return type;
  }

  /**
   * Returns the inclusive starting timestamp of the reporting period.
   *
   * @return the starting timestamp
   */
  public Timestamp getFrom() {
    return from;
  }

  /**
   * Returns the exclusive ending timestamp of the reporting period.
   *
   * @return the ending timestamp
   */
  public Timestamp getTo() {
    return to;
  }

  /**
   * Returns the quarter associated with this report.
   *
   * @return the report quarter, or {@code null} if the report is not
   *         quarter-based
   */
  public Quarter getQuarter() {
    return quarter;
  }

  /**
   * Returns the transactions included in this report.
   *
   * <p>The returned list is the original mutable list supplied to the
   * constructor.</p>
   *
   * @return the report's transaction list
   */
  public List<Transaction> getTransactionList() {
    return transactionList;
  }

  /**
   * Returns total income grouped by category name.
   *
   * @return map whose keys are category names and whose values are total
   *         income amounts
   */
  public Map<String, Double> getTotalIncomesByCategory() {
    return totalIncomesByCategory;
  }

  /**
   * Returns total expenses grouped by category name.
   *
   * @return map whose keys are category names and whose values are total
   *         expense amounts
   */
  public Map<String, Double> getTotalExpensesByCategory() {
    return totalExpensesByCategory;
  }

  /**
   * Returns total income grouped by month.
   *
   * @return map whose keys are months and whose values are total income
   *         amounts
   */
  public Map<Month, Double> getTotalIncomesByMonth() {
    return totalIncomesByMonth;
  }

  /**
   * Returns total expenses grouped by month.
   *
   * @return map whose keys are months and whose values are total expense
   *         amounts
   */
  public Map<Month, Double> getTotalExpensesByMonth() {
    return totalExpensesByMonth;
  }

  /**
   * Returns overall totals grouped by transaction type.
   *
   * <p>The map may not contain a transaction type when no transactions of that
   * type were included in the report.</p>
   *
   * @return map containing overall income and expense totals
   */
  public Map<TransactionType, Double> getTotals() {
    return totals;
  }

  /**
   * Returns average income and expense amounts per day.
   *
   * @return map containing daily averages grouped by transaction type
   */
  public Map<TransactionType, Double> getAveragesPerDay() {
    return averagesPerDay;
  }

  /**
   * Returns average income and expense amounts per month.
   *
   * @return map containing monthly averages grouped by transaction type
   */
  public Map<TransactionType, Double> getAveragesPerMonth() {
    return averagesPerMonth;
  }

  /**
   * Prepares the report by calculating the reporting period length, totals,
   * and averages.
   */
  private void prepare() {
    Log.d(REPORT, "Generating the report...");

    calculateNumberOfDaysAndMonths();
    calculateTotals();
    calculateAverages();
  }

  /**
   * Calculates the number of days and calendar months covered by the report.
   *
   * <p>Timestamps are converted using the device's default time zone. The
   * {@code from} timestamp is inclusive, while the {@code to} timestamp is
   * exclusive.</p>
   *
   * <p>The month count represents the number of calendar months touched by the
   * date range rather than the number of complete elapsed months.</p>
   */
  private void calculateNumberOfDaysAndMonths() {
    Log.d(REPORT, "Calculating number of days and months...");

    ZoneId userTimeZone = ZoneId.systemDefault();

    LocalDate localFrom = from
            .toInstant()
            .atZone(userTimeZone)
            .toLocalDate();

    LocalDate localToExclusive = to
            .toInstant()
            .atZone(userTimeZone)
            .toLocalDate();

    LocalDate localToInclusive = to
            .toInstant()
            .minusNanos(1)
            .atZone(userTimeZone)
            .toLocalDate();

    dayCount = ChronoUnit.DAYS.between(localFrom, localToExclusive);

    monthCount = ChronoUnit.MONTHS.between(
            YearMonth.from(localFrom),
            YearMonth.from(localToInclusive)
    ) + 1;
  }

  /**
   * Calculates all report totals.
   *
   * <p>Each transaction is grouped by:</p>
   * <ul>
   *   <li>Transaction type</li>
   *   <li>Category name</li>
   *   <li>Calendar month</li>
   * </ul>
   *
   * <p>Transaction amounts are rounded to two decimal places before being
   * added to the corresponding totals. Category names are retrieved from
   * {@link CacheService} using the transaction's category identifier.</p>
   */
  private void calculateTotals() {
    Log.d(REPORT, "Calculating totals...");

    for (Transaction transaction : transactionList) {
      String categoryName = CacheService
              .get(CacheKey.CATEGORIES, transaction.getCategoryID(),Category.class)
              .getName();

      TransactionType transactionType = transaction.getType();

      int monthValue = transaction
              .getCreatedOn()
              .toDate()
              .toInstant()
              .atZone(ZoneId.systemDefault())
              .toLocalDate()
              .getMonthValue();

      Month month = Month.parseMonth(monthValue);

      double amount = Math.round(transaction.getAmount() * 100.0) / 100.0;

      Double byTransactionTypeValue = totals.get(transactionType);
      Double byCategoryValue;
      Double byMonthValue;

      if (transactionType == TransactionType.INCOME) {
        byCategoryValue = totalIncomesByCategory.get(categoryName);
        byMonthValue = totalIncomesByMonth.get(month);

        if (byCategoryValue == null) {
          byCategoryValue = 0.0;
        }

        if (byMonthValue == null) {
          byMonthValue = 0.0;
        }

        totalIncomesByCategory.put(categoryName, byCategoryValue + amount);
        totalIncomesByMonth.put(month, byMonthValue + amount);
      } else {
        byCategoryValue = totalExpensesByCategory.get(categoryName);
        byMonthValue = totalExpensesByMonth.get(month);

        if (byCategoryValue == null) {
          byCategoryValue = 0.0;
        }

        if (byMonthValue == null) {
          byMonthValue = 0.0;
        }

        totalExpensesByCategory.put(categoryName, byCategoryValue + amount);
        totalExpensesByMonth.put(month, byMonthValue + amount);
      }

      if (byTransactionTypeValue == null) {
        byTransactionTypeValue = 0.0;
      }

      totals.put(transactionType, byTransactionTypeValue + amount);
    }
  }

  /**
   * Calculates daily and monthly averages for income and expenses.
   *
   * <p>When no transactions exist for a transaction type, its total is treated
   * as zero.</p>
   *
   * <p>This method assumes that {@link #dayCount} and {@link #monthCount} are
   * greater than zero. A zero-length reporting period may produce
   * {@link Double#NaN} or an infinite value.</p>
   */
  private void calculateAverages() {
    Log.d(REPORT, "Calculating averages...");

    Double totalIncome = totals.get(TransactionType.INCOME);
    Double totalExpense = totals.get(TransactionType.EXPENSE);

    if (totalIncome == null) {
      totalIncome = 0.0;
    }

    if (totalExpense == null) {
      totalExpense = 0.0;
    }

    averagesPerDay.put(
            TransactionType.INCOME,
            totalIncome / dayCount
    );

    averagesPerDay.put(
            TransactionType.EXPENSE,
            totalExpense / dayCount
    );

    averagesPerMonth.put(
            TransactionType.INCOME,
            totalIncome / monthCount
    );

    averagesPerMonth.put(
            TransactionType.EXPENSE,
            totalExpense / monthCount
    );
  }
}