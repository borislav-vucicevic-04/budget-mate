package com.borislavvucicevic.budgetmate.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.borislavvucicevic.budgetmate.MainActivity;
import com.borislavvucicevic.budgetmate.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class TransactionsActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_transactions);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
      Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
      return insets;
    });

    // grabbing widgets
    FloatingActionButton floatingActionButton = findViewById(R.id.floatingActionButton);

    // setting listeners
    floatingActionButton.setOnClickListener(v -> this.openTransactionsAddNewActivity());
  }

  private void openTransactionsAddNewActivity() {
    startActivity(new Intent(TransactionsActivity.this, TransactionsAddNewActivity.class));
  }
}