package com.borislavvucicevic.budgetmate.services;

import androidx.annotation.NonNull;

import com.borislavvucicevic.budgetmate.models.Category;
import com.borislavvucicevic.budgetmate.models.Transaction;
import com.borislavvucicevic.budgetmate.models.UserProfile;
import com.borislavvucicevic.budgetmate.enums.CacheKey;
import com.borislavvucicevic.budgetmate.exceptions.CacheException;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Provides a centralized, in-memory cache for application data.
 *
 * <p>The service stores data that is shared between different parts of the
 * application, including the authenticated user's profile, categories,
 * transactions, pagination state, and temporary transaction-editing data.</p>
 *
 * <p>All members and methods are static, so the cached data exists for the
 * lifetime of the current application process. The data is not persisted and
 * may be lost when the process is terminated.</p>
 *
 * <p>This class uses mutable static state and is not inherently thread-safe.
 * Callers must provide synchronization if the cache is accessed concurrently
 * from multiple threads.</p>
 */
public class CacheService {

  /**
   * Cached profile of the currently authenticated user.
   *
   * <p>The value is {@code null} when no profile has been loaded or when the
   * profile cache has been cleared.</p>
   */
  private static UserProfile userProfile;

  /**
   * Cached transactions indexed by their unique string identifiers.
   *
   * <p>Transactions without an identifier are not stored.</p>
   */
  private static final HashMap<String, Transaction> transactions = new HashMap<>();

  /**
   * Cached transaction categories indexed by their unique string identifiers.
   *
   * <p>Categories without an identifier are not stored.</p>
   */
  private static final HashMap<String, Category> categories = new HashMap<>();

  /**
   * Indicates whether another page of transactions is available.
   *
   * <p>The value defaults to {@code true} and may be {@code null} after the
   * corresponding cache entry has been cleared.</p>
   */
  private static Boolean hasNextPage = true;

  /**
   * Last Firestore document returned by a paginated transaction query.
   *
   * <p>This document can be used as the starting point for the next database
   * query. The value is {@code null} when pagination has not started or when
   * the pagination state has been cleared.</p>
   */
  private static DocumentSnapshot lastVisibleDocument = null;

  /**
   * Transaction currently being created or edited.
   *
   * <p>This value allows transaction data to be transferred between activities,
   * such as {@code TransactionsActivity} and
   * {@code TransactionUpsertActivity}, without immediately persisting it.</p>
   */
  private static Transaction transactionUpsertObject = null;

  /**
   * Position of the transaction being created or edited in the associated
   * RecyclerView.
   *
   * <p>The value is {@code null} when no RecyclerView position is currently
   * cached.</p>
   */
  private static Integer positionInView = null;

  /**
   * Prevents the creation of {@code CacheService} instances.
   *
   * <p>This class exposes only static cache operations and is therefore
   * intended to be used as a utility class.</p>
   */
  private CacheService() {}

  /**
   * Stores a value under the specified cache key.
   *
   * <p>The supplied object's runtime type must match the selected key:</p>
   *
   * <ul>
   *   <li>{@link CacheKey#USER_PROFILE} requires a {@link UserProfile}.</li>
   *   <li>{@link CacheKey#CATEGORIES} requires a {@link List} containing
   *       {@link Category} objects.</li>
   *   <li>{@link CacheKey#TRANSACTIONS} requires a {@link List} containing
   *       {@link Transaction} objects.</li>
   *   <li>{@link CacheKey#HAS_NEXT_PAGE} requires a {@link Boolean}.</li>
   *   <li>{@link CacheKey#LAST_VISIBLE_DOCUMENT} requires a
   *       {@link DocumentSnapshot}.</li>
   *   <li>{@link CacheKey#TRANSACTION_UPSERT_OBJECT} requires a
   *       {@link Transaction}.</li>
   *   <li>{@link CacheKey#POSITION_IN_VIEW} requires an {@link Integer}.</li>
   * </ul>
   *
   * <p>When categories or transactions are stored, they are added to or replace
   * records in their respective maps based on their identifiers. Existing map
   * entries that are not present in the supplied list are not removed.</p>
   *
   * @param key the cache key that identifies the destination cache entry
   * @param object the non-null value to store
   *
   * @throws IllegalArgumentException if {@code key} is {@code null}, if
   *         {@code object} is {@code null}, or if the object's type does not
   *         match the selected cache key
   */
  public static void store(CacheKey key, Object object) {
    if(key == null) throw new IllegalArgumentException("The cache key cannot be null.");
    if(object == null) throw new IllegalArgumentException("Cannot store null values in cache.");

    switch (key) {
      case USER_PROFILE:
        if(object instanceof UserProfile) userProfile = (UserProfile) object;
        else throw new IllegalArgumentException(
                "Object must be an instance of class UserProfile."
        );
        break;

      case CATEGORIES:
        if(object instanceof List<?>) CacheService.storeCategories((List<?>) object);
        else throw new IllegalArgumentException(
                "Object must be an instance of class List<Category>."
        );
        break;

      case TRANSACTIONS:
        if(object instanceof List<?>) CacheService.storeTransactions((List<?>) object);
        else throw new IllegalArgumentException(
                "Object must be an instance of class List<Transaction>."
        );
        break;

      case HAS_NEXT_PAGE:
        if(object instanceof Boolean) hasNextPage = (Boolean) object;
        else throw new IllegalArgumentException(
                "Object must be an instance of class Boolean."
        );
        break;

      case LAST_VISIBLE_DOCUMENT:
        if(object instanceof DocumentSnapshot) {
          lastVisibleDocument = (DocumentSnapshot) object;
        } else {
          throw new IllegalArgumentException(
                  "Object must be an instance of class DocumentSnapshot."
          );
        }
        break;

      case TRANSACTION_UPSERT_OBJECT:
        if(object instanceof Transaction) {
          transactionUpsertObject = (Transaction) object;
        } else {
          throw new IllegalArgumentException(
                  "Object must be an instance of class Transaction."
          );
        }
        break;

      case POSITION_IN_VIEW:
        if(object instanceof Integer) positionInView = (Integer) object;
        else throw new IllegalArgumentException(
                "Object must be an instance of class Integer."
        );
        break;
    }
  }

  /**
   * Reads a single cached value and casts it to the requested type.
   *
   * <p>This method is intended for scalar cache entries such as the user
   * profile, pagination state, last visible Firestore document, transaction
   * upsert object, and RecyclerView position.</p>
   *
   * <p>Categories and transactions must be retrieved using
   * {@link #readList(CacheKey)}.</p>
   *
   * @param key the cache key identifying the value to retrieve
   * @param type the expected runtime type of the cached value
   * @param <T> the expected return type
   *
   * @return the cached value cast to {@code T}, or {@code null} if no value is
   *         currently cached
   *
   * @throws IllegalArgumentException if {@code key} is {@code null}
   * @throws CacheException if {@code key} refers to categories or transactions
   * @throws ClassCastException if the cached value cannot be cast to
   *         {@code type}
   * @throws NullPointerException if {@code type} is {@code null}
   */
  public static <T> T read(CacheKey key, Class<T> type) {
    if(key == null) throw new IllegalArgumentException("The cache key cannot be null.");

    switch (key) {
      case USER_PROFILE:
        return type.cast(userProfile);

      case HAS_NEXT_PAGE:
        return type.cast(hasNextPage);

      case LAST_VISIBLE_DOCUMENT:
        return type.cast(lastVisibleDocument);

      case TRANSACTION_UPSERT_OBJECT:
        return type.cast(transactionUpsertObject);

      case POSITION_IN_VIEW:
        return type.cast(positionInView);

      case CATEGORIES:
        throw new CacheException(
                "You cannot use \"read\" method to read cached categories. "
                        + "You must use \"readList\" method"
        );

      case TRANSACTIONS:
        throw new CacheException(
                "You cannot use \"read\" method to read cached transactions. "
                        + "You must use \"readList\" method"
        );

      default:
        return null;
    }
  }

  /**
   * Returns the cached categories or transactions as a new list.
   *
   * <p>The returned list is a snapshot of the map values at the time this
   * method is called. Modifying the returned list does not modify the cache,
   * although the objects contained in the list are the same object instances
   * stored in the cache.</p>
   *
   * <p>The order of elements is not guaranteed because the underlying values
   * are stored in {@link HashMap} instances.</p>
   *
   * @param key {@link CacheKey#CATEGORIES} or
   *            {@link CacheKey#TRANSACTIONS}
   * @param <T> the expected element type, normally {@link Category} or
   *            {@link Transaction}
   *
   * @return a new list containing the cached values, or an empty list when the
   *         selected cache contains no records or the key does not represent a
   *         supported collection
   *
   * @throws IllegalArgumentException if {@code key} is {@code null}
   */
  @NonNull
  @SuppressWarnings("unchecked")
  public static <T> List<T> readList(CacheKey key) {
    if(key == null) throw new IllegalArgumentException("The cache key cannot be null.");

    switch (key) {
      case CATEGORIES:
        return new ArrayList<>(
                (Collection<? extends T>) categories.values()
        );

      case TRANSACTIONS:
        return new ArrayList<>(
                (Collection<? extends T>) transactions.values()
        );

      default:
        return new ArrayList<>();
    }
  }

  /**
   * Clears the cached value associated with the specified key.
   *
   * <p>For categories and transactions, all records in the corresponding map
   * are removed. For other keys, the cached reference is set to {@code null}.</p>
   *
   * @param key the cache key identifying the value or collection to clear
   *
   * @throws IllegalArgumentException if {@code key} is {@code null}
   */
  public static void clear(CacheKey key) {
    if(key == null) throw new IllegalArgumentException("The cache key cannot be null.");

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

      case HAS_NEXT_PAGE:
        hasNextPage = null;
        break;

      case LAST_VISIBLE_DOCUMENT:
        lastVisibleDocument = null;

      case TRANSACTION_UPSERT_OBJECT:
        transactionUpsertObject = null;
        break;

      case POSITION_IN_VIEW:
        positionInView = null;
    }
  }

  /**
   * Clears every value currently held by the cache.
   *
   * <p>All category and transaction records are removed, and all other cached
   * references are set to {@code null}.</p>
   */
  public static void clearAll() {
    userProfile = null;
    categories.clear();
    transactions.clear();
    hasNextPage = null;
    lastVisibleDocument = null;
    transactionUpsertObject = null;
    positionInView = null;
  }

  /**
   * Adds or replaces a single category or transaction in the cache.
   *
   * <p>The supplied key must represent either the category cache or the
   * transaction cache. The runtime type of {@code object} determines which map
   * receives the object. An existing record with the same identifier is
   * replaced.</p>
   *
   * @param key a collection cache key; either {@link CacheKey#CATEGORIES} or
   *            {@link CacheKey#TRANSACTIONS}
   * @param object the {@link Category} or {@link Transaction} to cache
   *
   * @throws CacheException if {@code key} does not represent categories or
   *         transactions, or if the supplied object does not have an identifier
   * @throws IllegalArgumentException if {@code object} is {@code null} or is
   *         neither a {@link Category} nor a {@link Transaction}
   */
  public static void put(CacheKey key, Object object) {
    if(key != CacheKey.CATEGORIES && key != CacheKey.TRANSACTIONS) {
      throw new CacheException(
              "You can use \"put\" method only when updating cached categories "
                      + "or transactions"
      );
    }

    if(object == null) {
      throw new IllegalArgumentException("Cannot cache null values.");
    }

    if(object instanceof Category) {
      Category category = (Category) object;

      if(category.getID() != null) {
        categories.put(category.getID(), category);
      } else {
        throw new CacheException(
                "The category object is malformed. It does not have an ID"
        );
      }
    } else if(object instanceof Transaction) {
      Transaction transaction = (Transaction) object;

      if(transaction.getID() != null) {
        transactions.put(transaction.getID(), transaction);
      } else {
        throw new CacheException(
                "The transaction object is malformed. It does not have an ID"
        );
      }
    } else {
      throw new IllegalArgumentException(
              "Object must be an instance of class Category or Transaction."
      );
    }
  }

  /**
   * Retrieves a single category or transaction by its identifier.
   *
   * @param key the collection to search; either
   *            {@link CacheKey#CATEGORIES} or
   *            {@link CacheKey#TRANSACTIONS}
   * @param ID the unique identifier of the requested record
   * @param type the expected runtime type of the returned record
   * @param <T> the expected return type
   *
   * @return the matching cached record cast to {@code T}, or {@code null} if no
   *         record exists for the supplied identifier
   *
   * @throws CacheException if {@code key} does not represent categories or
   *         transactions
   * @throws ClassCastException if the cached record cannot be cast to
   *         {@code type}
   * @throws NullPointerException if {@code type} is {@code null}
   */
  public static <T> T get(CacheKey key, String ID, Class<T> type) {
    if(key == CacheKey.CATEGORIES) {
      return type.cast(categories.getOrDefault(ID, null));
    } else if(key == CacheKey.TRANSACTIONS) {
      return type.cast(transactions.getOrDefault(ID, null));
    } else {
      throw new CacheException(
              "You can use \"get\" method only when getting a record from "
                      + "cached categories or transactions"
      );
    }
  }

  /**
   * Removes a single category or transaction from the cache.
   *
   * <p>If no record exists for the supplied identifier, the cache remains
   * unchanged.</p>
   *
   * @param key the collection from which the record should be removed; either
   *            {@link CacheKey#CATEGORIES} or
   *            {@link CacheKey#TRANSACTIONS}
   * @param ID the unique identifier of the record to remove
   *
   * @throws CacheException if {@code key} does not represent categories or
   *         transactions
   */
  public static void remove(CacheKey key, String ID) {
    if(key == CacheKey.CATEGORIES) {
      categories.remove(ID);
    } else if(key == CacheKey.TRANSACTIONS) {
      transactions.remove(ID);
    } else {
      throw new CacheException(
              "You can use \"remove\" method only when removing a record from "
                      + "cached categories or transactions"
      );
    }
  }

  /**
   * Adds a collection of categories to the category cache.
   *
   * <p>Each category is indexed by its unique identifier. Existing records with
   * matching identifiers are replaced. Categories whose identifiers are
   * {@code null} are ignored. Existing cached categories that are not included
   * in the supplied list remain unchanged.</p>
   *
   * @param categoryList the non-null list of categories to cache
   *
   * @throws IllegalArgumentException if an element in the list is
   *         {@code null} or is not an instance of {@link Category}
   */
  private static void storeCategories(@NonNull List<?> categoryList) {
    if(categoryList.isEmpty()) return;

    for (Object object : categoryList) {
      if(!(object instanceof Category)) {
        throw new IllegalArgumentException(
                "List contains object that is not an instance of class Category."
        );
      }

      Category category = (Category) object;

      if(category.getID() != null) {
        categories.put(category.getID(), category);
      }
    }
  }

  /**
   * Adds a collection of transactions to the transaction cache.
   *
   * <p>Each transaction is indexed by its unique identifier. Existing records
   * with matching identifiers are replaced. Transactions whose identifiers are
   * {@code null} are ignored. Existing cached transactions that are not
   * included in the supplied list remain unchanged.</p>
   *
   * @param transactionList the non-null list of transactions to cache
   *
   * @throws IllegalArgumentException if an element in the list is
   *         {@code null} or is not an instance of {@link Transaction}
   */
  private static void storeTransactions(@NonNull List<?> transactionList) {
    if(transactionList.isEmpty()) return;

    for (Object object : transactionList) {
      if(!(object instanceof Transaction)) {
        throw new IllegalArgumentException(
                "List contains object that is not an instance of class Transaction."
        );
      }

      Transaction transaction = (Transaction) object;

      if(transaction.getID() != null) {
        transactions.put(transaction.getID(), transaction);
      }
    }
  }
}