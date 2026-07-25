package com.borislavvucicevic.budgetmate.models.classes;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

/**
 * Represents a single page of transaction results returned from a paginated query.
 * <p>
 * This class encapsulates the transactions retrieved for the current page, the
 * last visible Firestore document used as the pagination cursor, and a flag
 * indicating whether additional pages of results are available.
 */
public class TransactionPage {
  private final List<Transaction> transactionList;
  private final DocumentSnapshot lastVisibleDocument;
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