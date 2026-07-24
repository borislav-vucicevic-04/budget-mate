package com.borislavvucicevic.budgetmate.models.classes;

import com.google.firebase.firestore.DocumentId;

/**
 * Represents a budget category within the BudgetMate application.
 * This model class is designed for seamless serialization and deserialization
 * with Google Cloud Firestore.
 *
 * <p>It maps directly to documents inside the Firestore "categories" collection,
 * allowing users to define custom labels (such as Groceries, Utilities, or Entertainment)
 * for organizing their transactions.</p>
 */
public class Category {
  /**
   * The unique identifier of the category.
   * */
  @DocumentId
  private String ID;
  /**
   * The display name of the category.
   */
  private String name;

  /**
   * The unique identifier of the user who created or owns this category.
   */
  private String userID;

  /**
   * Default no-argument constructor.
   * Required by Cloud Firestore to instantiate the object before inflating its fields.
   */
  public Category() { /* EMPTY CONSTRUCTOR REQUIRED BY THE FIREBASE */ }

  /**
   * Constructs a new Category with the specified identifier, name, and owner.
   *
   * @param ID     the unique identifier for this category (e.g., a UUID or database primary key)
   * @param name   the display name or title of the category
   * @param userID the unique identifier of the user who owns or created this category
   */
  public Category(String ID, String name, String userID) {
    this.ID = ID;
    this.name = name;
    this.userID = userID;
  }

  /**
   * Gets the unique identifier of the category
   *
   * @return the category ID string
   * */
  public String getID() {
    return ID;
  }

  /**
   * Sets the unique identifier of the category.
   *
   * @param ID the category name string to set.
   */
  public void setID(String ID) {
    this.ID = ID;
  }

  /**
   * Gets the display name of the category.
   *
   * @return the category name string.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the display name of the category.
   *
   * @param name the category name string to set.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the unique identifier of the user who owns this category.
   *
   * @return the user ID string.
   */
  public String getUserID() {
    return userID;
  }

  /**
   * Sets the unique identifier of the user who owns this category.
   *
   * @param userID the user ID string to set.
   */
  public void setUserID(String userID) {
    this.userID = userID;
  }
}
