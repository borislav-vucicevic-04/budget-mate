package com.borislavvucicevic.budgetmate.models;

import com.borislavvucicevic.budgetmate.enums.CacheKey;
import com.borislavvucicevic.budgetmate.enums.ReportType;
import com.borislavvucicevic.budgetmate.enums.TransactionType;
import com.borislavvucicevic.budgetmate.services.CacheService;

import java.time.LocalDate;
import java.util.List;

/**
 * Represents a financial report for a single calendar day.
 *
 * <p>This report extends {@link Report} and is configured with
 * {@link ReportType#DAILY}. The same date is used as both the start and end
 * date of the report period.</p>
 *
 * <p>When a new instance is created, the report immediately prepares its
 * calculated data by invoking {@link #prepare()}. Transaction totals are
 * calculated both by {@link TransactionType} and by transaction category.</p>
 *
 * <p>Category information is retrieved from {@link CacheService} using the
 * transaction's stored category identifier.</p>
 *
 * @see Report
 * @see Transaction
 * @see TransactionType
 * @see CacheService
 */
public class DailyReport extends Report {

  /**
   * Creates a new daily report for the specified date and transaction list.
   *
   * <p>The supplied date is used as both the beginning and end of the report
   * period. After the superclass has been initialized, {@link #prepare()} is
   * called to calculate the report totals.</p>
   *
   * @param date date represented by this daily report
   * @param transactionList transactions included in the report
   */
  public DailyReport(LocalDate date, List<Transaction> transactionList) {
    super(ReportType.DAILY, date, date, transactionList);
    prepare();
  }

  /**
   * Prepares the report data for use.
   *
   * <p>For a daily report, preparation consists of calculating transaction
   * totals by calling {@link #calculateTotals()}.</p>
   */
  @Override
  protected void prepare() {
    calculateTotals();
  }

  /**
   * Calculates transaction totals for the daily report.
   *
   * <p>Each transaction is processed individually. The transaction category
   * is resolved from {@link CacheService} using
   * {@link CacheKey#CATEGORIES} and the transaction's category identifier.</p>
   *
   * <p>The transaction amount is rounded to two decimal places and then
   * accumulated in the overall totals map according to whether the
   * transaction is an {@link TransactionType#INCOME} or
   * {@link TransactionType#EXPENSE}.</p>
   *
   * <p>Amounts are also grouped by category. Income transactions are stored
   * in {@code totalIncomesByCategory}, while expense transactions are stored
   * in {@code totalExpensesByCategory}.</p>
   */
  @Override
  protected void calculateTotals() {
    for (Transaction transaction : transactionList) {
      String categoryName = CacheService
              .get(CacheKey.CATEGORIES, transaction.getCategoryID(),Category.class)
              .getName();

      TransactionType transactionType = transaction.getType();
      double amount = Math.round(transaction.getAmount() * 100.0) / 100.0;

      Double byTransactionTypeValue = totals.get(transactionType);

      if (byTransactionTypeValue == null) {
        byTransactionTypeValue = 0.0;
      }

      totals.put(transactionType, byTransactionTypeValue + amount);

      Double byCategoryValue = transactionType == TransactionType.INCOME ?
              totalIncomesByCategory.get(categoryName) :
              totalExpensesByCategory.get(categoryName);


      if (byCategoryValue == null) {
        byCategoryValue = 0.0;
      }

      if (transactionType == TransactionType.INCOME) {
        totalIncomesByCategory.put(categoryName, byCategoryValue + amount);
      } else {
        totalExpensesByCategory.put(categoryName, byCategoryValue + amount);
      }
    }
  }
}