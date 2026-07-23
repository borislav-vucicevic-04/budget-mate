package com.borislavvucicevic.budgetmate.services;

import android.util.Log;

import com.borislavvucicevic.budgetmate.models.classes.Category;
import com.borislavvucicevic.budgetmate.models.classes.UserProfile;
import com.borislavvucicevic.budgetmate.models.enums.CacheKey;
import com.borislavvucicevic.budgetmate.models.exceptions.DatabaseException;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

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
}