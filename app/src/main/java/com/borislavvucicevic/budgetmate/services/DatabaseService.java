package com.borislavvucicevic.budgetmate.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.borislavvucicevic.budgetmate.models.classes.Category;
import com.borislavvucicevic.budgetmate.models.classes.Transaction;
import com.borislavvucicevic.budgetmate.models.classes.TransactionPage;
import com.borislavvucicevic.budgetmate.models.classes.UserProfile;
import com.borislavvucicevic.budgetmate.models.enums.CacheKey;
import com.borislavvucicevic.budgetmate.models.exceptions.DatabaseException;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class DatabaseService {
  public static final String DATABASE = "DATABASE";
  private final FirebaseFirestore firestore;

  public DatabaseService() {
    firestore = FirebaseFirestore.getInstance();
  }

  public void createUserProfile(String uid, UserProfile userProfile) throws DatabaseException {
    try {
      DocumentReference documentReference = firestore.collection("users").document(uid);

      Map<String, Object> user = new HashMap<>();
      user.put("fullName", userProfile.getFullName());
      user.put("email", userProfile.getEmail());
      user.put("homeCurrency", userProfile.getHomeCurrency());

      Tasks.await(documentReference.set(user));
      CacheService.storeUserProfile(userProfile);
      Log.d(DATABASE, "Profile creation successfully executed.");
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      String errorMessage = (cause != null) ? cause.getMessage() : "Unknown Firestore error occurred";
      Log.e(DATABASE, "Write failed: " + errorMessage, cause);
      throw new DatabaseException(errorMessage, cause);
    } catch (InterruptedException e) {
      Log.e(DATABASE, "The write operation thread was interrupted.", e);
      Thread.currentThread().interrupt(); // Restore standard thread interrupt flag
      throw new DatabaseException("Database operation was interrupted before completion.", e);
    }
  }

  /**
   * Retrieves the user profile for the specified user ID, prioritizing the local memory cache.
   * <p>
   * This method first checks the {@link CacheService} to see if the profile is already available
   * in memory. If found, it returns the cached instance immediately to eliminate network latency
   * and save Firestore read costs. If it is a cache miss, the method performs a synchronous
   * network call to fetch the live document from Cloud Firestore, maps it to a {@link UserProfile}
   * object, and updates the cache for future requests.
   * </p>
   * <p>
   * @param uid the unique identifier (Firebase Auth UID) of the user whose profile is being requested
   * @return a {@link UserProfile} object containing the user's information
   * @throws DatabaseException if the document does not exist on the server, if the network operation
   *                           fails, or if the thread is interrupted while waiting
   */
  public UserProfile getUserProfile(String uid) throws DatabaseException {
    UserProfile cachedProfile = CacheService.readUserProfile();
    // 1. Return immediately if we already have it in memory
    if (cachedProfile != null) return cachedProfile;

    try {
      DocumentReference documentReference = firestore.collection("users").document(uid);

      // Synchronously wait for the document fetch
      DocumentSnapshot snapshot = Tasks.await(documentReference.get());

      if (snapshot.exists()) {
        // Save to memory cache so the next activity gets it for $0 billing reads
        cachedProfile = snapshot.toObject(UserProfile.class);
        CacheService.storeUserProfile(cachedProfile);
        return cachedProfile;
      } else {
        throw new DatabaseException("User profile document does not exist on the server.", null);
      }

    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      String errorMessage = (cause != null) ? cause.getMessage() : "Unknown Firestore error occurred";
      Log.e(DATABASE, "Fetch failed: " + errorMessage, cause);
      throw new DatabaseException(errorMessage, cause);
    } catch (InterruptedException e) {
      Log.e(DATABASE, "The fetch operation thread was interrupted.", e);
      Thread.currentThread().interrupt();
      throw new DatabaseException("Database operation was interrupted before completion.", e);
    }
  }

  /**
   * Retrieves all custom budget categories belonging to a specific user.
   * Checks the local cache first to minimize database reads, falling back to a
   * synchronous Firestore collection query if the cache is empty.
   *
   * @param uid the unique identifier of the user whose categories are being retrieved.
   * @return a {@link List} of {@link Category} objects matching the user ID.
   * @throws DatabaseException if the network query fails or is interrupted.
   */
  public List<Category> getCategories(String uid) throws DatabaseException {
    // Return immediately if we already have them in memory
    List<Category> cachedCategories = CacheService.readCategories();
    if (!cachedCategories.isEmpty()) {
      return cachedCategories;
    }

    try {
      Log.d(DATABASE, "Users id (that must not be null): " + uid);
      // Build the query to filter categories by the specific user's ID
      Query query = firestore.collection("categories")
              .whereEqualTo("userID", uid);

      // Synchronously wait for the query snapshot fetch
      QuerySnapshot snapshot = Tasks.await(query.get());

      // Map the documents to your Category model class
      List<Category> categoriesList = snapshot.toObjects(Category.class);

      // Save to your cache system so subsequent lookups read from local memory
      CacheService.storeCategories(categoriesList);

      return categoriesList;

    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      String errorMessage = (cause != null) ? cause.getMessage() : "Unknown Firestore query error occurred";
      Log.e(DATABASE, "Category fetch failed: " + errorMessage, cause);
      throw new DatabaseException(errorMessage, cause);
    } catch (InterruptedException e) {
      Log.e(DATABASE, "The category fetch operation thread was interrupted.", e);
      Thread.currentThread().interrupt();
      throw new DatabaseException("Database operation was interrupted before completion.", e);
    }
  }

  public void insertTransaction(Transaction transaction) throws DatabaseException {
    try {
      Category category = transaction.getCategory();
      String resolvedCategoryID;

      // 1. Handle Category Logic
      if (category.getID() == null) {
        DocumentReference newCategoryRef = firestore.collection("categories").document();
        resolvedCategoryID = newCategoryRef.getId();

        category.setID(resolvedCategoryID);

        Map<String, Object> categoryMap = new HashMap<>();
        categoryMap.put("userID", transaction.getUserID());
        categoryMap.put("name", category.getName());

        Tasks.await(newCategoryRef.set(categoryMap));
        CacheService.putCategory(category);
        Log.d(DATABASE, "Category successfully inserted with generated ID: " + resolvedCategoryID);
      } else {
        resolvedCategoryID = category.getID();
      }

      // 2. Handle Transaction Logic
      DocumentReference newTransactionRef = firestore.collection("transactions").document();
      transaction.setID(newTransactionRef.getId());
      transaction.setCategoryID(resolvedCategoryID);

      Map<String, Object> transactionMap = new HashMap<>();
      transactionMap.put("userID", transaction.getUserID());
      transactionMap.put("amount", transaction.getAmount());
      transactionMap.put("categoryID", resolvedCategoryID);
      transactionMap.put("createdOn", transaction.getCreatedOn());
      transactionMap.put("type", transaction.getType());

      // Conditionally add optional fields to avoid inserting null values into Firestore
      if (transaction.getNotes() != null) {
        transactionMap.put("notes", transaction.getNotes());
      }

      Tasks.await(newTransactionRef.set(transactionMap));
      CacheService.putTransaction(transaction);
      Log.d(DATABASE, "Transaction successfully inserted with ID: " + transaction.getID());
      Log.d(DATABASE, "insertTransaction completed successfully.");

    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      String errorMessage = (cause != null) ? cause.getMessage() : "Unknown Firestore error occurred";
      Log.e(DATABASE, "Transaction insertion failed: " + errorMessage, cause);
      throw new DatabaseException(errorMessage, cause);
    } catch (InterruptedException e) {
      Log.e(DATABASE, "The transaction write operation thread was interrupted.", e);
      Thread.currentThread().interrupt();
      throw new DatabaseException("Database operation was interrupted before completion.", e);
    }
  }

  public TransactionPage getTransactions(@NotNull String uid, int pageSize, DocumentSnapshot lastVisibleDocument) throws DatabaseException {
    if (pageSize <= 0) {
      throw new IllegalArgumentException("Page size must be bigger than 0.");
    }

    try {
      Log.d(DATABASE, "Loading user's transactions");
      /*
       * Request one additional document so that we can determine
       * whether another page exists without performing another query.
       */
      long queryLimit = (long) pageSize + 1L;

      // creating a query
      Query query = firestore
              .collection("transactions")
              .whereEqualTo("userID", uid)
              .orderBy("createdOn", Query.Direction.DESCENDING)
              .limit(queryLimit);

      /*
       * A null cursor means that this is the first page.
       */
      if (lastVisibleDocument != null) {
        query = query.startAfter(lastVisibleDocument);
      }

      QuerySnapshot snapshot = Tasks.await(query.get());
      List<DocumentSnapshot> documents = snapshot.getDocuments();
      boolean hasNextPage = documents.size() > pageSize;

      /*
       * The extra document is used only to detect whether another
       * page exists. Do not include it in the returned page.
       */
      int returnedDocumentCount =
              Math.min(pageSize, documents.size());

      List<Transaction> transactionList = new ArrayList<>(returnedDocumentCount);

      for(int i = 0; i < returnedDocumentCount; i++) {
        DocumentSnapshot document = documents.get(i);
        Transaction transaction = document.toObject(Transaction.class);

        if(transaction != null) {
          transactionList.add(transaction);
        } else {
          Log.w(DATABASE, "Could not convert document snapshot to object. ID:  " + document.getId());
        }
      }

      DocumentSnapshot newLastVisibleDocument = returnedDocumentCount > 0 ? documents.get(returnedDocumentCount - 1) : null;
      Log.d(
              DATABASE,
              "Loaded "
                      + transactionList.size()
                      + " transactions. Has next page: "
                      + hasNextPage
      );

      return new TransactionPage(transactionList, newLastVisibleDocument, hasNextPage);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();

      String errorMessage =
              cause != null && cause.getMessage() != null
                      ? cause.getMessage()
                      : "Unknown Firestore transaction query error occurred.";

      Log.e(DATABASE, "Transaction fetch failed: " + errorMessage, cause);

      throw new DatabaseException(errorMessage, cause);
    } catch (InterruptedException e) {
      Log.e(DATABASE, "The transaction fetch operation was interrupted.", e);

      Thread.currentThread().interrupt();

      throw new DatabaseException("Database operation was interrupted before completion.", e);
    }
  }

  /**
   * Deletes a transaction from Firestore using its unique document ID.
   *
   * This method blocks while waiting for Firestore, so it must be called
   * from a background thread.
   *
   * @param transactionID the unique Firestore document ID
   * @throws DatabaseException if the deletion fails or is interrupted
   */
  public void deleteTransaction(String transactionID) throws DatabaseException {
    if (transactionID == null || transactionID.trim().isEmpty()) {
      throw new DatabaseException("Transaction ID cannot be null or empty.", null);
    }

    try {
      DocumentReference transactionReference = firestore
              .collection("transactions")
              .document(transactionID);

      // Wait until Firestore confirms that the document was deleted.
      Tasks.await(transactionReference.delete());

      // Update the local cache only after the database operation succeeds.
      CacheService.removeTransaction(transactionID);

      Log.d(DATABASE, "Transaction successfully deleted with ID: " + transactionID);
      Log.d(DATABASE, "deleteTransaction completed successfully.");
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();

      String errorMessage =
              cause != null && cause.getMessage() != null
                      ? cause.getMessage()
                      : "Unknown Firestore deletion error occurred.";

      Log.e(DATABASE, "Transaction deletion failed: " + errorMessage, cause);

      throw new DatabaseException(errorMessage, cause);
    } catch (InterruptedException e) {
      Log.e(DATABASE, "The transaction deletion operation thread was interrupted.", e);

      Thread.currentThread().interrupt();

      throw new DatabaseException("Database operation was interrupted before completion.", e);
    }
  }
}