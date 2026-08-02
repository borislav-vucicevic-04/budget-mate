package com.borislavvucicevic.budgetmate.models;

import com.borislavvucicevic.budgetmate.enums.ReportType;
import com.borislavvucicevic.budgetmate.enums.TransactionType;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the common foundation for financial reports.
 *
 * <p>A report contains a report type, a reporting period, and a collection of
 * transactions used to calculate financial totals.</p>
 *
 * <p>This class also stores shared calculated values, including income totals
 * by category, expense totals by category, and general report totals.</p>
 *
 * <p>Because this class is abstract, it cannot be instantiated directly.
 * Concrete report implementations must define how the report is prepared and
 * how its totals are calculated.</p>
 */
public abstract class Report {

  /**
   * Type of this report.
   */
  private final ReportType type;

  /**
   * First date included in the reporting period.
   */
  private final LocalDate from;

  /**
   * Last date included in the reporting period.
   */
  private final LocalDate to;

  /**
   * Transactions included in this report.
   */
  protected final List<Transaction> transactionList;

  /**
   * Income totals grouped by category name.
   *
   * <p>Each key represents a category name, while the corresponding value
   * represents the total income assigned to that category.</p>
   */
  protected final Map<String, Double> totalIncomesByCategory = new HashMap<>();

  /**
   * Expense totals grouped by category name.
   *
   * <p>Each key represents a category name, while the corresponding value
   * represents the total expense assigned to that category.</p>
   */
  protected final Map<String, Double> totalExpensesByCategory = new HashMap<>();

  /**
   * General financial totals calculated for this report.
   *
   * <p>The meaning of each key is determined by the concrete report
   * implementation.</p>
   */
  protected final Map<TransactionType, Double> totals = new HashMap<>();

  /**
   * Creates a report with the specified type, reporting period, and
   * transactions.
   *
   * <p>This constructor is protected because reports must be instantiated
   * through concrete subclasses.</p>
   *
   * @param type            the type of report
   * @param from            the first date included in the reporting period
   * @param to              the last date included in the reporting period
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
   * Returns the first date included in the reporting period.
   *
   * @return the starting date of the report
   */
  public LocalDate getFrom() {
    return from;
  }

  /**
   * Returns the last date included in the reporting period.
   *
   * @return the ending date of the report
   */
  public LocalDate getTo() {
    return to;
  }

  /**
   * Returns the transactions included in this report.
   *
   * <p>The returned list is the same list supplied to the constructor.
   * Modifying the list may therefore affect the report.</p>
   *
   * @return the transactions included in the report
   */
  public List<Transaction> getTransactionList() {
    return transactionList;
  }

  /**
   * Returns income totals grouped by category name.
   *
   * <p>The returned map is mutable. Changes made to it directly affect the
   * report's stored income totals.</p>
   *
   * @return a map of category names to total income amounts
   */
  public Map<String, Double> getTotalIncomesByCategory() {
    return totalIncomesByCategory;
  }

  /**
   * Returns expense totals grouped by category name.
   *
   * <p>The returned map is mutable. Changes made to it directly affect the
   * report's stored expense totals.</p>
   *
   * @return a map of category names to total expense amounts
   */
  public Map<String, Double> getTotalExpensesByCategory() {
    return totalExpensesByCategory;
  }

  /**
   * Returns the general totals calculated for this report.
   *
   * <p>The keys and their meanings are defined by the concrete report
   * implementation. The returned map is mutable, and changes made to it
   * directly affect the report.</p>
   *
   * @return a map containing the report's calculated totals
   */
  public Map<TransactionType, Double> getTotals() {
    return totals;
  }

  /**
   * Prepares this report by executing the calculations required by the
   * concrete report implementation.
   *
   * <p>Subclasses determine which calculations are performed and the order in
   * which they are executed.</p>
   */
  protected abstract void prepare();

  /**
   * Calculates the financial totals for the transactions included in this
   * report.
   *
   * <p>Subclasses determine how transactions are grouped and which calculated
   * values are stored in the report's totals maps.</p>
   */
  protected abstract void calculateTotals();
}
