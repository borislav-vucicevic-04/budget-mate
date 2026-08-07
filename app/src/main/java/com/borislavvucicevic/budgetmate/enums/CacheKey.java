package com.borislavvucicevic.budgetmate.enums;

/**
 * Defines the keys used by the application's cache system.
 *
 * <p>Each enum constant identifies a specific type of data that can be
 * stored, retrieved, or cleared through the application's cache service.</p>
 *
 * <p>These keys are used to cache user-related information, transaction
 * data, pagination state, and temporary objects shared between activities.</p>
 *
 * @see com.borislavvucicevic.budgetmate.services.CacheService
 */
public enum CacheKey {

  /**
   * Key used to store the authenticated user's profile information.
   */
  USER_PROFILE,

  /**
   * Key used to store the list of categories belonging to the current user.
   */
  CATEGORIES,

  /**
   * Key used to store cached transaction data.
   */
  TRANSACTIONS,

  /**
   * Key used to store whether another page of transactions is available
   * during pagination.
   */
  HAS_NEXT_PAGE,

  /**
   * Key used to store the last visible database document used as the
   * pagination cursor when loading transactions.
   */
  LAST_VISIBLE_DOCUMENT,

  /**
   * Key used to temporarily store a transaction that has been created,
   * updated, or selected for editing.
   *
   * <p>This value can be used to transfer the transaction between the
   * transaction list and transaction upsert activities.</p>
   */
  TRANSACTION_UPSERT_OBJECT,

  /**
   * Key used to store the position of a transaction within the displayed
   * transaction list.
   */
  POSITION_IN_VIEW
}
