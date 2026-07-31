package com.borislavvucicevic.budgetmate.enums;

/**
 * Represents one of the four quarters of a calendar or financial year.
 *
 * <p>Each quarter is associated with a numeric value from {@code 1} to
 * {@code 4}.</p>
 */
public enum Quarter {

  /**
   * The first quarter of the year.
   */
  I(1),

  /**
   * The second quarter of the year.
   */
  II(2),

  /**
   * The third quarter of the year.
   */
  III(3),

  /**
   * The fourth quarter of the year.
   */
  IV(4);

  /**
   * The numeric value associated with this quarter.
   */
  private final int quarterNumber;

  /**
   * Creates a quarter with the specified numeric value.
   *
   * @param quarterNumber the quarter number, ranging from {@code 1} to {@code 4}
   */
  Quarter(int quarterNumber) {
    this.quarterNumber = quarterNumber;
  }

  /**
   * Returns the numeric value of this quarter.
   *
   * @return the quarter number, ranging from {@code 1} to {@code 4}
   */
  public int getQuarterNumber() {
    return this.quarterNumber;
  }

  public int getStartMonthNumber() {
    return (quarterNumber - 1) * 3 + 1;
  }

  public int getEndMonthNumber() {
    return quarterNumber * 3;
  }
}