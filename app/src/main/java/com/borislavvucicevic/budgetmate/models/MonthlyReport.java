package com.borislavvucicevic.budgetmate.models;

import com.borislavvucicevic.budgetmate.enums.Month;
import com.borislavvucicevic.budgetmate.enums.ReportType;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Represents a financial report for a specific calendar month and year.
 *
 * <p>The reporting period begins on the first day of the specified month and
 * ends on the last day of that month. Transaction totals and average values
 * are calculated using the functionality inherited from
 * {@link AveragedReport}.</p>
 *
 * <p>The report is prepared automatically during construction by the
 * {@code AveragedReport} superclass.</p>
 */
public class MonthlyReport extends AveragedReport {

  /**
   * Calendar month represented by this report.
   */
  private final Month month;

  /**
   * Calendar year represented by this report.
   */
  private final Integer year;

  /**
   * Creates a monthly report for the specified month and year.
   *
   * <p>The reporting period is automatically set from the first day through
   * the last day of the specified month.</p>
   *
   * @param month the calendar month represented by the report
   * @param year the calendar year represented by the report
   * @param transactionList the transactions included in the report
   */
  public MonthlyReport(Month month, Integer year, List<Transaction> transactionList) {
    super(
            ReportType.MONTHLY,
            // calculating the first date covered by the report, which is the first day of the month
            LocalDate.of(year, month.getMonthValue(), 1),
            // calculating the last date covered by the report, which is the last days of the month
            LocalDate.of(year, month.getMonthValue(), 1).with(TemporalAdjusters.lastDayOfMonth()),
            transactionList
    );
    this.month = month;
    this.year = year;
  }

  /**
   * Returns the calendar month represented by this report.
   *
   * @return the report month
   */
  public Month getMonth() {
    return month;
  }

  /**
   * Returns the calendar year represented by this report.
   *
   * @return the report year
   */
  public Integer getYear() {
    return year;
  }
}