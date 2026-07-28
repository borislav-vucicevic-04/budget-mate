package com.borislavvucicevic.budgetmate.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.borislavvucicevic.budgetmate.models.classes.Category;
import com.borislavvucicevic.budgetmate.models.classes.Transaction;
import com.borislavvucicevic.budgetmate.models.classes.UserProfile;
import com.borislavvucicevic.budgetmate.models.enums.CacheKey;
import com.borislavvucicevic.budgetmate.models.exceptions.CacheException;
import com.google.firebase.firestore.DocumentSnapshot;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * A static, centralized caching service for managing global application state.
 * This class provides convenient methods to store, retrieve, update, and clear
 * user profiles, categories, and transaction data throughout the life of the application.
 */
public class CacheService {

  /**
   * The cached profile information of the currently authenticated user.
   */
  private static UserProfile userProfile;

  /**
   * The cached indicator indicating if there are more transactions to be loaded.
   * */
  private static boolean hasNextPage = true;

  /**
   * The cached last document that has been fetched from the database to improve query fetching performance
   * */
  private static DocumentSnapshot lastVisibleDocument = null;

  /**
   * A map storing user-defined transaction categories, indexed by their unique String ID.
   */
  private static final HashMap<String, Category> categories = new HashMap<>();

  /**
   * A map storing the loaded user transactions, indexed by their unique String ID.
   */
  private static final HashMap<String, Transaction> transactions = new HashMap<>();

  /**
   * Private constructor to enforce utility class pattern and prevent instantiation.
   */
  private CacheService() {}

  /**
   * Stores a loaded user profile into the cache.
   *
   * @param profile the {@link UserProfile} object to cache
   */
  public static void storeUserProfile(@NonNull UserProfile profile) {
    userProfile = profile;
  }

  /**
   * Stores indicator indicating if there are more transactions to be loaded
   *
   * @param hasNextPage the boolean value indicating if there are more transactions to be loaded.
   * */
  public static void storeHasNextPage(boolean hasNextPage) {
    CacheService.hasNextPage = hasNextPage;
  }


  /**
   * Stores last document fetched from the database, that is used to improve query performance
   * and allow pagination.
   *
   * @param documentSnapshot the last document fetched from the database.
   * */
  public static void storeLastVisibleDocument(DocumentSnapshot documentSnapshot) {
    lastVisibleDocument = documentSnapshot;
  }

  /**
   * Stores a list of loaded user-defined categories into the cache.
   * Iterates through the list and maps each category by its unique ID.
   * Null items or items with null IDs inside the list are safely ignored.
   *
   * @param categoryList the list of {@link Category} objects to map and store
   */
  public static void storeCategories(@NonNull List<Category> categoryList) {
    for (Category category : categoryList) {
      if (category != null && category.getID() != null) {
        categories.put(category.getID(), category);
      }
    }
  }

  /**
   * Stores a list of loaded user transactions into the cache.
   * Iterates through the list and maps each transaction by its unique ID.
   * Null items or items with null IDs inside the list are safely ignored.
   *
   * @param transactionList the list of {@link Transaction} objects to map and store
   */
  public static void storeTransactions(@NonNull List<Transaction> transactionList) {
    for (Transaction transaction : transactionList) {
      if (transaction != null && transaction.getID() != null) {
        transactions.put(transaction.getID(), transaction);
      }
    }
  }

  /**
   * Adds or updates a single category document inside the categories cache.
   * The document is stored using its unique ID as the mapping key.
   * Documents with missing IDs will be ignored.
   *
   * @param document the {@link Category} record to place into the cache
   */
  public static void putCategory(@NonNull Category document) {
    if (document.getID() != null) {
      categories.put(document.getID(), document);
    }
  }

  /**
   * Adds or updates a single transaction document inside the transactions cache.
   * The document is stored using its unique ID as the mapping key.
   * Documents with missing IDs will be ignored.
   *
   * @param document the {@link Transaction} record to place into the cache
   */
  public static void putTransaction(@NonNull Transaction document) {
    if (document.getID() != null) {
      transactions.put(document.getID(), document);
    }
  }

  /**
   * Retrieves a single record from the categories cache based on its unique ID string.
   * If no record is found, it returns null.
   *
   * @param id the unique String identifier of the category to retrieve
   * */
  public static Category getCategory(@NonNull String id) {
    return categories.getOrDefault(id, null);
  }

  /**
   * Safely removes a record from the categories cache based on its unique ID string.
   * Fails silently without throwing an exception if the record does not exist in the hashmap.
   *
   * @param id the unique String identifier of the category to remove
   */
  public static void removeCategory(@NonNull String id) {
    categories.remove(id);
  }

  /**
   * Safely removes a record from the transactions cache based on its unique ID string.
   * Fails silently without throwing an exception if the record does not exist in the hashmap.
   *
   * @param id the unique String identifier of the transaction to remove
   */
  public static void removeTransaction(@NonNull String id) {
    transactions.remove(id);
  }

  /**
   * Retrieves the currently cached user profile.
   *
   * @return the cached {@link UserProfile} object, or {@code null} if no profile is loaded
   */
  public static UserProfile readUserProfile() {
    return userProfile;
  }

  /**
   * Retrieves the currently cached indicator indicating if there are more transactions to be loaded
   *
   * @return cached boolean indicator.
   * */
  public static boolean readHasNextPage() {
    return hasNextPage;
  }

  /**
   * Retrieves the cached last document fetched from the database.
   *
   * @return the cached {@link DocumentSnapshot} object, or {@code null} if no documents have been fetched
   * */
  public static DocumentSnapshot readLastVisibleDocument() {
    return lastVisibleDocument;
  }

  /**
   * Exposes the contents of the categories cache converted into a sequential collection.
   *
   * @return a new {@link List} containing all currently cached {@link Category} objects
   */
  public static List<Category> readCategories() {
    return new ArrayList<>(categories.values());
  }

  /**
   * Exposes the contents of the transactions cache converted into a sequential collection.
   *
   * @return a new {@link List} containing all currently cached {@link Transaction} objects
   */
  public static List<Transaction> readTransactions() {
    ArrayList<Transaction> list = new ArrayList<>(transactions.values());
    list.sort(Comparator.comparing(Transaction::getCreatedOn).reversed());
    return list;
  }

  /**
   * Clears a specific memory cache target depending on the provided key value.
   *
   * @param key the {@link CacheKey} targeting a specific internal field to wipe
   */
  public static void clear(@NonNull CacheKey key) {
    switch (key) {
      case USER_PROFILE:
        userProfile = null;
        break;
      case CATEGORIES:
        categories.clear();
        break;
      case TRANSACTIONS:
        transactions.clear();
        break;
    }
  }

  /**
   * Clears all fields inside this cache service simultaneously, releasing memory references.
   * Resets the user profile to {@code null} and clears out both internal map structures.
   */
  public static void clearAll() {
    userProfile = null;
    categories.clear();
    transactions.clear();
  }
}