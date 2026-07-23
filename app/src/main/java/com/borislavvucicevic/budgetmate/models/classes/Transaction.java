package com.borislavvucicevic.budgetmate.models.classes;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

/**
 * Represents a financial transaction within the BudgetMate application.
 * This model class is designed for seamless serialization and deserialization
 * with Google Cloud Firestore.
 *
 * <p>It maps directly to documents inside the Firestore "transactions" collection,
 * safely handling optional properties like notes and automatically mapping the
 * document's unique database identifier.</p>
 */
public class Transaction {

  /**
   * The unique identifier of the Firestore document.
   * Annotated with {@link DocumentId} to automatically populate the ID
   * without needing it as an explicit data field in the document.
   */
  @DocumentId
  private String ID;

  /**
   * The unique identifier of the user who owns this transaction.
   */
  private String userID;

  /**
   * The monetary value of the transaction.
   */
  private Float amount;

  /**
   * The unique identifier of the budget category associated with this transaction.
   */
  private String categoryID;

  /**
   * The full Category object mapping to this transaction.
   * Annotated with @Exclude to prevent Firestore from attempting to serialize the nested object into the transaction document.
   */
  @Exclude
  private Category category;

  /**
   * Optional custom descriptions, memos, or details regarding the transaction.
   * Can be null if no notes were provided by the user.
   */
  private String notes;

  /**
   * The precise timestamp indicating when this transaction record was originally created.
   */
  private Timestamp createdOn;

  /**
   * The precise timestamp indicating when this transaction record was last modified.
   */
  private Timestamp modifiedOn;

  /**
   * Default no-argument constructor.
   * Required by Cloud Firestore to instantiate the object before inflating its fields.
   */
  public Transaction() {
    /* EMPTY CONSTRUCTOR REQUIRED BY FIREBASE */
  }

  /**
   * Gets the unique Firestore document identifier.
   *
   * @return the unique document ID string.
   */
  public String getID() {
    return ID;
  }

  /**
   * Sets the unique Firestore document identifier.
   *
   * @param ID the unique document ID string to set.
   */
  public void setID(String ID) {
    this.ID = ID;
  }

  /**
   * Gets the unique identifier of the user who owns this transaction.
   *
   * @return the user ID string.
   */
  public String getUserID() {
    return userID;
  }

  /**
   * Sets the unique identifier of the user who owns this transaction.
   *
   * @param userID the user ID string to set.
   */
  public void setUserID(String userID) {
    this.userID = userID;
  }

  /**
   * Gets the monetary value of the transaction.
   *
   * @return the transaction amount.
   */
  public Float getAmount() {
    return amount;
  }

  /**
   * Sets the monetary value of the transaction.
   *
   * @param amount the transaction amount to set.
   */
  public void setAmount(Float amount) {
    this.amount = amount;
  }

  /**
   * Gets the unique identifier of the budget category associated with this transaction.
   *
   * @return the transaction category ID string.
   */
  public String getCategoryID() {
    return categoryID;
  }

  /**
   * Sets the unique identifier of the budget category associated with this transaction.
   *
   * @param categoryID the transaction category ID string to set.
   */
  public void setCategoryID(String categoryID) {
    this.categoryID = categoryID;
  }

  /**
   * Gets the full Category object associated with this transaction.
   * Annotated with @Exclude to avoid Firestore database conflicts.
   *
   * @return the Category object.
   */
  @Exclude
  public Category getCategory() {
    return category;
  }

  /**
   * Sets the full Category object associated with this transaction.
   * Annotated with @Exclude to avoid Firestore database conflicts.
   *
   * @param category the Category object to set.
   */
  @Exclude
  public void setCategory(Category category) {
    this.category = category;
  }

  /**
   * Gets the optional notes or description of the transaction.
   *
   * @return the transaction notes string, or null if empty.
   */
  public String getNotes() {
    return notes;
  }

  /**
   * Sets the optional notes or description of the transaction.
   *
   * @param notes the transaction notes string to set.
   */
  public void setNotes(String notes) {
    this.notes = notes;
  }

  /**
   * Gets the Firestore timestamp indicating when the transaction was created.
   *
   * @return the creation {@link Timestamp}.
   */
  public Timestamp getCreatedOn() {
    return createdOn;
  }

  /**
   * Sets the Firestore timestamp indicating when the transaction was created.
   *
   * @param createdOn the creation {@link Timestamp} to set.
   */
  public void setCreatedOn(Timestamp createdOn) {
    this.createdOn = createdOn;
  }

  /**
   * Gets the Firestore timestamp indicating when the transaction was last modified.
   *
   * @return the modification {@link Timestamp}.
   */
  public Timestamp getModifiedOn() {
    return modifiedOn;
  }

  /**
   * Sets the Firestore timestamp indicating when the transaction was last modified.
   *
   * @param modifiedOn the modification {@link Timestamp} to set.
   */
  public void setModifiedOn(Timestamp modifiedOn) {
    this.modifiedOn = modifiedOn;
  }
}