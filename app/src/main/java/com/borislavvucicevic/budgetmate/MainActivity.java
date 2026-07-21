package com.borislavvucicevic.budgetmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.borislavvucicevic.budgetmate.activities.TransactionsActivity;
import com.borislavvucicevic.budgetmate.activities.UserProfileActivity;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_main);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activityLogin), (v, insets) -> {
      Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
      return insets;
    });

    // grabbing widgets
    Button btnUserProfile = findViewById(R.id.btnUserProfile);
    Button btnViewTransactions = findViewById(R.id.btnViewTransactions);

    // setting listeners
    btnUserProfile.setOnClickListener(v -> this.openUserProfileActivity());
    btnViewTransactions.setOnClickListener(v -> this.openTransactionsActivity());
  }
  private void openUserProfileActivity() {
    startActivity(new Intent(MainActivity.this, UserProfileActivity.class));
  }
  private void openTransactionsActivity() {
    startActivity(new Intent(MainActivity.this, TransactionsActivity.class));
  }
}