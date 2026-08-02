package com.borislavvucicevic.budgetmate.enums;

/**
 * Represents a month of the year.
 *
 * <p>Each month is associated with a one-based numeric value, where
 * {@code 1} represents January and {@code 12} represents December.</p>
 */
public enum Month {

  /** January, represented by the value {@code 1}. */
  JAN(1),

  /** February, represented by the value {@code 2}. */
  FEB(2),

  /** March, represented by the value {@code 3}. */
  MAR(3),

  /** April, represented by the value {@code 4}. */
  APR(4),

  /** May, represented by the value {@code 5}. */
  MAY(5),

  /** June, represented by the value {@code 6}. */
  JUN(6),

  /** July, represented by the value {@code 7}. */
  JUL(7),

  /** August, represented by the value {@code 8}. */
  AUG(8),

  /** September, represented by the value {@code 9}. */
  SEP(9),

  /** October, represented by the value {@code 10}. */
  OCT(10),

  /** November, represented by the value {@code 11}. */
  NOV(11),

  /** December, represented by the value {@code 12}. */
  DEC(12);

  /** The one-based numeric value associated with this month. */
  private final int monthValue;

  /**
   * Creates a month with the specified one-based numeric value.
   *
   * @param monthValue the month value, ranging from {@code 1} for January
   *     to {@code 12} for December
   */
  Month(int monthValue) {
    this.monthValue = monthValue;
  }

  /**
   * Returns the one-based numeric value of this month.
   *
   * @return the month value, ranging from {@code 1} to {@code 12}
   */
  public int getMonthValue() {
    return this.monthValue;
  }

  /**
   * Returns the {@link Month} corresponding to the specified numeric month value.
   *
   * @param monthValue the numeric month value, from {@code 1} for January
   *                   to {@code 12} for December
   * @return the matching {@link Month} enum constant
   * @throws IllegalArgumentException if {@code monthValue} is outside the range
   *                                  {@code 1} to {@code 12}
   */
  public static Month parseMonth(int monthValue) {
    for (Month month : Month.values()) {
      if (month.monthValue == monthValue) {
        return month;
      }
    }
    throw new IllegalArgumentException("Month values must be between 1 and 12");
  }
}