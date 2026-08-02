package com.borislavvucicevic.budgetmate.models;

import com.borislavvucicevic.budgetmate.enums.CacheKey;
import com.borislavvucicevic.budgetmate.enums.Month;
import com.borislavvucicevic.budgetmate.enums.ReportType;
import com.borislavvucicevic.budgetmate.enums.TransactionType;
import com.borislavvucicevic.budgetmate.services.CacheService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a financial report that calculates totals and average amounts
 * over a specified reporting period.
 *
 * <p>In addition to the information inherited from {@link Report}, this class
 * calculates:</p>
 *
 * <ul>
 *   <li>The number of days covered by the reporting period</li>
 *   <li>The number of calendar months covered by the reporting period</li>
 *   <li>Income and expense totals grouped by month</li>
 *   <li>Average income and expense amounts per day</li>
 *   <li>Average income and expense amounts per month</li>
 * </ul>
 *
 * <p>The report is prepared automatically when an instance is created.</p>
 */
public class AveragedReport extends Report {

  /**
   * Number of days between the starting and ending dates of the report.
   *
   * <p>The ending date is treated as exclusive when this value is
   * calculated.</p>
   */
  protected long dayCount;

  /**
   * Number of calendar months covered by the reporting period.
   *
   * <p>Both the starting and ending months are included.</p>
   */
  protected long monthCount;

  /**
   * Income totals grouped by calendar month.
   */
  protected final Map<Month, Double> totalIncomesByMonth = new HashMap<>();

  /**
   * Expense totals grouped by calendar month.
   */
  protected final Map<Month, Double> totalExpensesByMonth = new HashMap<>();

  /**
   * Average income and expense amounts per day, grouped by transaction type.
   */
  protected final Map<TransactionType, Double> averagesPerDay = new HashMap<>();

  /**
   * Average income and expense amounts per month, grouped by transaction type.
   */
  protected final Map<TransactionType, Double> averagesPerMonth = new HashMap<>();

  /**
   * Creates and prepares an averaged financial report.
   *
   * <p>The report preparation process calculates the duration of the reporting
   * period, transaction totals, and daily and monthly averages.</p>
   *
   * @param type the type of report
   * @param from the starting date of the reporting period
   * @param to the ending date of the reporting period
   * @param transactionList the transactions included in the report
   */
  public AveragedReport(ReportType type, LocalDate from, LocalDate to, List<Transaction> transactionList) {
    super(type, from, to, transactionList);
    prepare();
  }

  /**
   * Prepares the report by calculating the reporting-period duration,
   * transaction totals, and average amounts.
   */
  @Override
  protected void prepare() {
    calculateNumberOfDaysAndMonths();
    calculateTotals();
    calculateAverages();
  }

  /**
   * Calculates the number of days and calendar months covered by the reporting
   * period.
   *
   * <p>The number of days is calculated between the starting date and the
   * exclusive ending date. The number of months includes both the month
   * containing the starting date and the month containing the ending date.</p>
   */
  private void calculateNumberOfDaysAndMonths() {
    dayCount = ChronoUnit.DAYS.between(getFrom(), getTo());
    monthCount = ChronoUnit.MONTHS.between(YearMonth.from(getFrom()), YearMonth.from(getTo())) + 1;
  }

  /**
   * Calculates transaction totals grouped by transaction type, category, and
   * calendar month.
   *
   * <p>Each transaction amount is rounded to two decimal places before being
   * added to the appropriate totals. Category names are retrieved from
   * {@link CacheService} using the transaction's category identifier.</p>
   *
   * <p>Transaction dates are converted using the system's default time
   * zone.</p>
   */
  @Override
  protected void calculateTotals() {
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

      if (byTransactionTypeValue == null) {
        byTransactionTypeValue = 0.0;
      }

      Double byCategoryValue = transactionType == TransactionType.INCOME ?
              totalIncomesByCategory.get(categoryName) :
              totalExpensesByCategory.get(categoryName);
      
      if (byCategoryValue == null) {
        byCategoryValue = 0.0;
      }

      Double byMonthValue = transactionType == TransactionType.INCOME ?
              totalIncomesByMonth.get(month) :
              totalExpensesByMonth.get(month);

      if (byMonthValue == null) {
        byMonthValue = 0.0;
      }

      if (transactionType == TransactionType.INCOME) {
        totalIncomesByCategory.put(categoryName, byCategoryValue + amount);
        totalIncomesByMonth.put(month, byMonthValue + amount);
      } else {
        totalExpensesByCategory.put(categoryName, byCategoryValue + amount);
        totalExpensesByMonth.put(month, byMonthValue + amount);
      }

      totals.put(transactionType, byTransactionTypeValue + amount);
    }
  }

  /**
   * Calculates average income and expense amounts per day and per month.
   *
   * <p>If no transactions exist for a transaction type, its total is treated
   * as zero. This method assumes that {@link #dayCount} and
   * {@link #monthCount} contain valid nonzero values.</p>
   */
  private void calculateAverages() {
    Double totalIncome = totals.get(TransactionType.INCOME);
    Double totalExpense = totals.get(TransactionType.EXPENSE);

    if (totalIncome == null) {
      totalIncome = 0.0;
    }

    if (totalExpense == null) {
      totalExpense = 0.0;
    }

    averagesPerDay.put(TransactionType.INCOME, totalIncome / dayCount);
    averagesPerDay.put(TransactionType.EXPENSE, totalExpense / dayCount);
    averagesPerMonth.put(TransactionType.INCOME, totalIncome / monthCount);
    averagesPerMonth.put(TransactionType.EXPENSE, totalExpense / monthCount);
  }
}