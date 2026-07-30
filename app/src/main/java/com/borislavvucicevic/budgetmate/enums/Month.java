package com.borislavvucicevic.budgetmate.enums;

/**
 * Represents a month of the year.
 *
 * <p>Each month is associated with a zero-based numeric value, where
 * {@code 0} represents January and {@code 11} represents December.</p>
 */
public enum Month {

  /** January, represented by the value {@code 0}. */
  JAN(0),

  /** February, represented by the value {@code 1}. */
  FEB(1),

  /** March, represented by the value {@code 2}. */
  MAR(2),

  /** April, represented by the value {@code 3}. */
  APR(3),

  /** May, represented by the value {@code 4}. */
  MAY(4),

  /** June, represented by the value {@code 5}. */
  JUN(5),

  /** July, represented by the value {@code 6}. */
  JUL(6),

  /** August, represented by the value {@code 7}. */
  AUG(7),

  /** September, represented by the value {@code 8}. */
  SEP(8),

  /** October, represented by the value {@code 9}. */
  OCT(9),

  /** November, represented by the value {@code 10}. */
  NOV(10),

  /** December, represented by the value {@code 11}. */
  DEC(11);

  /** The zero-based numeric value associated with this month. */
  private final int monthValue;

  /**
   * Creates a month with the specified zero-based numeric value.
   *
   * @param monthValue the month value, ranging from {@code 0} for January
   *     to {@code 11} for December
   */
  Month(int monthValue) {
    this.monthValue = monthValue;
  }

  /**
   * Returns the zero-based numeric value of this month.
   *
   * @return the month value, ranging from {@code 0} to {@code 11}
   */
  public int getMonthValue() {
    return this.monthValue;
  }
}