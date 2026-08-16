package com.borislavvucicevic.budgetmate.models;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

/**
 * Represents a single page of transaction results returned from a paginated query.
 * <p>
 * This class encapsulates the transactions retrieved for the current page, the
 * last visible Firestore document used as the pagination cursor, and a flag
 * indicating whether additional pages of results are available.
 * </p>
 *
 * <p>Instances of this class are used to transfer both transaction data and
 * pagination metadata between the database layer and the application.</p>
 *
 * @see Transaction
 * @see DocumentSnapshot
 */
public class TransactionPage {
  public static final int PAGE_SIZE = 20;
  /**
   * List of transactions contained in the current page.
   */
  private final List<Transaction> transactionList;

  /**
   * Last visible Firestore document retrieved for the current page.
   *
   * <p>This document is used as the pagination cursor when requesting
   * the next page of transactions.</p>
   */
  private final DocumentSnapshot lastVisibleDocument;

  /**
   * Indicates whether additional pages of transactions are available.
   */
  private final boolean hasNextPage;

  /**
   * Creates a new {@code TransactionPage}.
   *
   * @param transactionList the transactions contained in the current page
   * @param lastVisibleDocument the last visible Firestore document in the current page,
   *                            used as the cursor for loading the next page;
   *                            may be {@code null} if no transactions were returned
   * @param hasNextPage {@code true} if additional pages of transactions are available;
   *                    {@code false} otherwise
   */
  public TransactionPage(List<Transaction> transactionList, DocumentSnapshot lastVisibleDocument, boolean hasNextPage) {
    this.transactionList = transactionList;
    this.lastVisibleDocument = lastVisibleDocument;
    this.hasNextPage = hasNextPage;
  }

  /**
   * Returns the transactions contained in this page.
   *
   * @return the list of transactions
   */
  public List<Transaction> getTransactionList() {
    return transactionList;
  }

  /**
   * Returns the last visible Firestore document for this page.
   * <p>
   * This document can be used as the pagination cursor when retrieving the next page.
   * </p>
   *
   * @return the last visible document, or {@code null} if the page is empty
   */
  public DocumentSnapshot getLastVisibleDocument() {
    return lastVisibleDocument;
  }

  /**
   * Indicates whether additional pages of transactions are available.
   *
   * @return {@code true} if another page can be loaded; {@code false} otherwise
   */
  public boolean hasNextPage() {
    return hasNextPage;
  }
}