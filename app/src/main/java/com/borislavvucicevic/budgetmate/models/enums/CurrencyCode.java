package com.borislavvucicevic.budgetmate.models.enums;

/**
 * Represents the standard ISO-4217-aligned currency codes supported by the application.
 * <p>
 * This enumeration defines the international financial currencies utilized for currency
 * conversion, transaction logging, and wallet budget management within the system.
 * </p>
 */
public enum CurrencyCode {
  /**
   * Bosnia and Herzegovina Convertible Mark.
   */
  BAM,
  /**
   * Serbian Dinar.
   */
  RSD,
  /**
   * Euro.
   */
  EUR,
  /**
   * United States Dollar.
   */
  USD;

  public static String parse(CurrencyCode code) {
    if(code == null) return "";

    switch (code) {
      case BAM: return "KM";
      case EUR: return "€";
      case USD: return "$";
      case RSD: return "DIN";
      default: return "";
    }
  }
}
