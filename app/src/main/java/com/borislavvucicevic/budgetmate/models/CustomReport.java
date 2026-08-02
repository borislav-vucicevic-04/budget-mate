package com.borislavvucicevic.budgetmate.models;

import com.borislavvucicevic.budgetmate.enums.ReportType;

import java.time.LocalDate;
import java.util.List;

/**
 * Represents a financial report for a custom reporting period.
 *
 * <p>The start and end dates of the reporting period are provided when the
 * report is created. Transaction totals, category totals, monthly totals,
 * and average income and expense values are calculated using the functionality
 * inherited from {@link AveragedReport}.</p>
 *
 * <p>The report type is automatically set to {@link ReportType#CUSTOM}.</p>
 */
public class CustomReport extends AveragedReport {

  /**
   * Creates a custom report for the specified reporting period.
   *
   * @param from the starting date of the reporting period
   * @param to the ending date of the reporting period
   * @param transactionList the transactions included in the report
   */
  public CustomReport(LocalDate from, LocalDate to, List<Transaction> transactionList) {
    super(ReportType.CUSTOM, from, to, transactionList);
  }
}
