package com.borislavvucicevic.budgetmate.schemas;

/**
 * Defines the field names for documents in the transactions collection.
 */
public final class TransactionsSchema {

  /**
   * The field containing the unique identifier of the user.
   */
  public static final String USER_ID = "userID";

  /**
   * The field containing the monetary amount of the transaction.
   */
  public static final String AMOUNT = "amount";

  /**
   * The field containing the reference identifier for the category.
   */
  public static final String CATEGORY_ID = "categoryID";

  /**
   * The field containing the timestamp when the transaction was created.
   */
  public static final String CREATED_ON = "createdOn";

  /**
   * The field containing the type of transaction (e.g., income or expense).
   */
  public static final String TYPE = "type";

  /**
   * The field containing optional user notes or descriptions.
   */
  public static final String NOTES = "notes";

  /**
   * The field containing the timestamp when the transaction was last modified.
   */
  public static final String MODIFIED_ON = "modifiedOn";
}