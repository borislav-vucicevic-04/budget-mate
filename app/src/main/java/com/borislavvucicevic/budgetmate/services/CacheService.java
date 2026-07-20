package com.borislavvucicevic.budgetmate.services;

import android.util.Log;

import com.borislavvucicevic.budgetmate.models.classes.UserProfile;
import com.borislavvucicevic.budgetmate.models.enums.CacheKey;
import com.borislavvucicevic.budgetmate.models.exceptions.CacheException;

/**
 * A centralized, in-memory caching service for the application.
 * <p>
 * This class provides global, static access to store, retrieve, and clear volatile
 * application data across different activities and background services without
 * triggering repeated, costly database operations.
 * </p>
 * <p>
 * Data managed by this service is bound to the application lifecycle and will be
 * completely wiped from memory when the application process terminates.
 * </p>
 */
public class CacheService {

  public static final String CACHE_SERVICE = "CACHE_SERVICE";
  /**
   * Cached instance of the current user's profile data.
   */
  private static UserProfile userProfile = null;

  /**
   * Stores an object in the in-memory cache associated with the specified cache key.
   * <p>
   * This method performs strict runtime type-checking before casting. If the provided
   * object does not match the expected data type for the given key, the operation
   * is aborted and a managed exception is thrown.
   * </p>
   *
   * @param key    the {@link CacheKey} identifying the type of data to be stored
   * @param object the data object to cache; must match the data type associated with the key
   * @throws CacheException if the provided object type does not match the type required by the key
   */
  public static void store(CacheKey key, Object object) throws CacheException {
    try {
      switch (key) {
        case USER:
          if (object instanceof UserProfile) userProfile = (UserProfile) object;
          else throw new ClassCastException("Expected UserProfile but received " + object.getClass().getSimpleName());
          break;
        default:
          /* DO NOTHING */
          break;
      }
    } catch (ClassCastException e) {
      Log.e(CACHE_SERVICE, "Cache insertion failed due to type mismatch.", e);
      throw new CacheException("Failed to store data in cache due to a type mismatch for key: " + key, e);
    }
  }


  /**
   * Retrieves an object from the in-memory cache associated with the specified cache key.
   *
   * @param key the {@link CacheKey} identifying the type of data to retrieve
   * @return the cached {@link Object} associated with the key, or {@code null} if no data is cached
   *         or if the key is unrecognized
   */
  public static Object read(CacheKey key) {
    switch (key) {
      case USER:
        return userProfile;
      default:
        return null;
    }
  }

  /**
   * Clears the cached data associated with a specific cache key, resetting it to {@code null}.
   *
   * @param key the {@link CacheKey} identifying the specific data to be evicted from the cache
   */
  public static void clear(CacheKey key) {
    switch (key) {
      case USER:
        userProfile = null;
        break;
      default:
        /* DO NOTHING */
        break;
    }
  }

  /**
   * Clears all cached data across all keys simultaneously.
   * <p>
   * This method should typically be invoked during user sign-out sequences to prevent
   * data leakage between different user sessions.
   * </p>
   */
  public static void clearAll() {
    userProfile = null;
  }
}
