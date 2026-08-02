package com.borislavvucicevic.budgetmate.models;

import com.borislavvucicevic.budgetmate.enums.Quarter;
import com.borislavvucicevic.budgetmate.enums.ReportType;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Represents a financial report for a specific quarter of a calendar year.
 *
 * <p>The reporting period begins on the first day of the quarter's starting
 * month and ends on the last day of the quarter's ending month.</p>
 *
 * <p>Transaction totals, category totals, monthly totals, and average values
 * are calculated using the functionality inherited from
 * {@link AveragedReport}.</p>
 */
public class QuarterlyReport extends AveragedReport {

  /**
   * Quarter represented by this report.
   */
  private final Quarter quarter;

  /**
   * Calendar year represented by this report.
   */
  private final Integer year;

  /**
   * Creates a quarterly report for the specified quarter and year.
   *
   * <p>The reporting period is automatically determined using the starting
   * and ending month numbers defined by the supplied {@link Quarter}.</p>
   *
   * @param quarter the quarter represented by the report
   * @param year the calendar year represented by the report
   * @param transactionList the transactions included in the report
   */
  public QuarterlyReport(Quarter quarter, Integer year, List<Transaction> transactionList) {
    super(
            ReportType.QUARTERLY,
            LocalDate.of(year, quarter.getStartMonthNumber(), 1),
            LocalDate.of(year, quarter.getEndMonthNumber(), 1).with(TemporalAdjusters.lastDayOfMonth()),
            transactionList
    );
    this.quarter = quarter;
    this.year = year;
  }

  /**
   * Returns the quarter represented by this report.
   *
   * @return the report quarter
   */
  public Quarter getQuarter() {
    return quarter;
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