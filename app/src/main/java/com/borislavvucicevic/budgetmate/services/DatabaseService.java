package com.borislavvucicevic.budgetmate.services;

import android.util.Log;

import com.borislavvucicevic.budgetmate.models.classes.UserProfile;
import com.borislavvucicevic.budgetmate.models.exceptions.DatabaseException;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
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
}