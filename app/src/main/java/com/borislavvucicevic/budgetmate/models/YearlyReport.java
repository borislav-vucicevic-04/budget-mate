package com.borislavvucicevic.budgetmate.models;

import com.borislavvucicevic.budgetmate.enums.ReportType;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Represents a financial report for a specific calendar year.
 *
 * <p>The reporting period begins on the first day of the specified year and
 * ends on the last day of that year.</p>
 *
 * <p>Transaction totals, category totals, monthly totals, and average income
 * and expense values are calculated using the functionality inherited from
 * {@link AveragedReport}.</p>
 */
public class YearlyReport extends AveragedReport {

  /**
   * Calendar year represented by this report.
   */
  private final Integer year;

  /**
   * Creates a yearly report for the specified calendar year.
   *
   * <p>The reporting period is automatically set from January 1 through
   * December 31 of the specified year.</p>
   *
   * @param year the calendar year represented by the report
   * @param transactionList the transactions included in the report
   */
  public YearlyReport(Integer year, List<Transaction> transactionList) {
    super(
            ReportType.YEARLY,
            LocalDate.of(year, 1, 1),
            LocalDate.of(year, 1, 1).with(TemporalAdjusters.lastDayOfYear()),
            transactionList
    );
    this.year = year;
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