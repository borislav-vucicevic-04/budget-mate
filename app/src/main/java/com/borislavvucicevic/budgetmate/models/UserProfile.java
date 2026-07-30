package com.borislavvucicevic.budgetmate.models;

import com.borislavvucicevic.budgetmate.enums.CurrencyCode;

/**
 * Represents the data model for an application user within the BudgetMate application.
 * This class is structured as a Plain Old Java Object (POJO) to enable seamless
 * serialization and deserialization with the Cloud Firestore SDK.
 *
 * <p>It maps directly to documents inside the "users" Firestore collection.</p>
 */
public class UserProfile {
  /**
   * The complete display name of the user.
   */
  private String fullName;

  /**
   * The primary contact email address of the user.
   */
  private String email;

  /**
   * The base currency code (e.g., "USD", "EUR") selected by the user for financial tracking.
   */
  private CurrencyCode homeCurrency;

  /**
   * Default no-argument constructor.
   * Required explicitly by the Cloud Firestore SDK to reconstruct the object
   * when executing {@code DocumentSnapshot.toObject(UserProfile.class)}.
   */
  public UserProfile() {
  }

  /**
   * Constructs a new UserProfile instance with the specified core profile details.
   * This constructor is typically utilized when initializing a brand-new account
   * before it is written to the remote database.
   *
   * @param fullName     the full name of the user
   * @param email        the email address of the user
   * @param homeCurrency the primary currency code used for budget calculations
   */
  public UserProfile(String fullName, String email, CurrencyCode homeCurrency) {
    this.fullName = fullName;
    this.email = email;
    this.homeCurrency = homeCurrency;
  }

  /**
   * Gets the complete display name of the user.
   *
   * @return the user's full name
   */
  public String getFullName() {
    return fullName;
  }

  /**
   * Sets the complete display name of the user.
   *
   * @param fullName the user's full name to set
   */
  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  /**
   * Gets the primary contact email address of the user.
   *
   * @return the user's email address
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets the primary contact email address of the user.
   *
   * @param email the user's email address to set
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * Gets the base currency code selected by the user.
   *
   * @return the three-letter currency code string
   */
  public CurrencyCode getHomeCurrency() {
    return homeCurrency;
  }

  /**
   * Sets the base currency code selected by the user.
   *
   * @param homeCurrency the three-letter currency code string to set
   */
  public void setHomeCurrency(CurrencyCode homeCurrency) {
    this.homeCurrency = homeCurrency;
  }
}