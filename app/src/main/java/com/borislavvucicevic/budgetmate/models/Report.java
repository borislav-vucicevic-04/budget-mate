package com.borislavvucicevic.budgetmate.models;

import com.borislavvucicevic.budgetmate.enums.ReportType;

import java.time.LocalDate;
import java.util.List;

/**
 * Defines the common state and behavior of a financial report.
 *
 * <p>Each report has a type, a reporting period, and a collection of
 * transactions from which its financial values are calculated.</p>
 *
 * <p>This class cannot be instantiated directly. Concrete subclasses must
 * define how the report is prepared and how transaction totals are
 * calculated.</p>
 */
public abstract class Report {

  /**
   * Type of this report.
   */
  private final ReportType type;

  /**
   * Starting date of the reporting period.
   */
  private final LocalDate from;

  /**
   * Ending date of the reporting period.
   */
  private final LocalDate to;

  /**
   * Transactions included in this report.
   */
  private final List<Transaction> transactionList;

  /**
   * Creates a report with the specified type, reporting period, and
   * transactions.
   *
   * <p>The constructor is protected because reports must be instantiated
   * through concrete subclasses.</p>
   *
   * @param type            the type of report
   * @param from            the starting date of the reporting period
   * @param to              the ending date of the reporting period
   * @param transactionList the transactions included in the report
   */
  protected Report(
          ReportType type,
          LocalDate from,
          LocalDate to,
          List<Transaction> transactionList
  ) {
    this.type = type;
    this.from = from;
    this.to = to;
    this.transactionList = transactionList;
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
   * Returns the starting date of the reporting period.
   *
   * @return the starting date
   */
  public LocalDate getFrom() {
    return from;
  }

  /**
   * Returns the ending date of the reporting period.
   *
   * @return the ending date
   */
  public LocalDate getTo() {
    return to;
  }

  /**
   * Returns the transactions included in this report.
   *
   * <p>The returned list is the same list supplied to the constructor.
   * Modifying it may therefore affect the report.</p>
   *
   * @return the list of transactions
   */
  public List<Transaction> getTransactionList() {
    return transactionList;
  }

  /**
   * Prepares this report by performing all calculations required by its
   * concrete report type.
   *
   * <p>Subclasses determine which calculation steps are necessary and the
   * order in which they are executed.</p>
   */
  protected abstract void prepare();

  /**
   * Calculates the financial totals for the transactions included in this
   * report.
   *
   * <p>Subclasses determine which totals are calculated and how transactions
   * are grouped.</p>
   */
  protected abstract void calculateTotals();
}