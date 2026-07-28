package com.borislavvucicevic.budgetmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.models.classes.Category;
import com.borislavvucicevic.budgetmate.models.classes.Transaction;
import com.borislavvucicevic.budgetmate.models.classes.TransactionCardAdapter;
import com.borislavvucicevic.budgetmate.models.classes.TransactionPage;
import com.borislavvucicevic.budgetmate.services.AuthService;
import com.borislavvucicevic.budgetmate.services.CacheService;
import com.borislavvucicevic.budgetmate.services.DatabaseService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class TransactionsActivity extends AppCompatActivity {
  private final String TRANSACTION_ACTIVITY = "TRANSACTION_ACTIVITY";
  private final int PAGE_SIZE = 20;
  private final AuthService authService = new AuthService();
  private final DatabaseService databaseService = new DatabaseService();
  private final ArrayList<Transaction> transactionList = new ArrayList<>(CacheService.readTransactions());
  private DocumentSnapshot lastVisibleDocument = CacheService.readLastVisibleDocument();
  private boolean hasNextPage = CacheService.readHasNextPage();
  private boolean isLoadingTransactions = false;
  private TransactionCardAdapter adapter;
  private RecyclerView recyclerView;
  private LinearLayout processIndicator;
  private TextView tvProcessMessage;

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
    recyclerView = findViewById(R.id.recyclerView);
    processIndicator = findViewById(R.id.processIndicator);
    tvProcessMessage = findViewById(R.id.tvProcessMessage);
    FloatingActionButton floatingActionButton = findViewById(R.id.floatingActionButton);

    // setting listeners
    floatingActionButton.setOnClickListener(v -> this.openTransactionsAddNewActivity());

    // initializing adapter
    adapter = new TransactionCardAdapter(this, transactionList, (id, position) -> this.handleDeleteTransaction(id, position));

    // setting up the recycler view
    this.setUpRecyclerView();

    // load transactions
    if (transactionList.isEmpty()) {
      this.loadTransactions();
    }
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    CacheService.storeHasNextPage(this.hasNextPage);
    CacheService.storeLastVisibleDocument(this.lastVisibleDocument);
  }

  private void setUpRecyclerView() {
    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.setAdapter(adapter);
    recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override
      public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        if(dy <= 0) {
          return;
        }

        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

        if (layoutManager == null) {
          return;
        }

        int totalItemCount = layoutManager.getItemCount();
        int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
        boolean isNearEnd = lastVisiblePosition >= totalItemCount - 5;

        if(isNearEnd && hasNextPage && !isLoadingTransactions) {
          loadTransactions();
        }
      }
    });
  }
  private void openTransactionsAddNewActivity() {
    startActivity(new Intent(TransactionsActivity.this, TransactionsAddNewActivity.class));
  }

  private void toggleProcessIndicator(Integer resourceStringID) {
    processIndicator.setVisibility(
            processIndicator.getVisibility() == View.GONE ?
                    View.VISIBLE :
                    View.GONE
    );

    if(resourceStringID != null) {
      tvProcessMessage.setText(getString(resourceStringID));
    }
  }
  private void loadTransactions() {
    // Exit immediately if loading is in process, or there are no more transactions to load
    if(this.isLoadingTransactions || !this.hasNextPage) {
      return;
    }
    this.isLoadingTransactions = true;
    // show toast
    Toast.makeText(
            TransactionsActivity.this,
            getString(R.string.transactions_loading),
            Toast.LENGTH_SHORT
    ).show();
    // fetch transaction in background
    Executors.newSingleThreadExecutor().execute(() -> {
      try {
        String uid = authService.getUserID();
        TransactionPage page = databaseService.getTransactions(uid, PAGE_SIZE, this.lastVisibleDocument);
        this.hasNextPage = page.hasNextPage();
        this.lastVisibleDocument = page.getLastVisibleDocument();
        List<Transaction> loadedTransactions = page.getTransactionList();

        // fetching transaction categories
        for(Transaction transaction : loadedTransactions) {
          Category category = CacheService.getCategory(transaction.getCategoryID());
          transaction.setCategory(category != null ? category : new Category(transaction.getUserID(), null, "No category"));
        }

        this.transactionList.addAll(loadedTransactions);
        CacheService.storeTransactions(loadedTransactions);
        runOnUiThread(() -> this.loadSuccess(transactionList.size() - loadedTransactions.size(), loadedTransactions.size()));
      } catch (Exception exception) {
        runOnUiThread(() -> this.handleException(exception));
      } finally {
        // opening the loading gate
        this.isLoadingTransactions = false;
      }
    });
  }
  private void loadSuccess(int positionStart, int itemCount) {
    Toast.makeText(
            TransactionsActivity.this,
            getString(R.string.transactions_loaded),
            Toast.LENGTH_SHORT
    ).show();
    // refreshing data
    adapter.notifyItemRangeInserted(positionStart, itemCount);
  }
  private void handleException(Exception exception) {
    Toast.makeText(
            TransactionsActivity.this,
            getString(R.string.error_general),
            Toast.LENGTH_SHORT
    ).show();
    Log.e(
            TRANSACTION_ACTIVITY,
            exception.getMessage(),
            exception
    );
  }

  private void handleDeleteTransaction(@NonNull String ID, int position) {
    this.toggleProcessIndicator(R.string.deleting_transaction);
    Executors.newSingleThreadExecutor().execute(() -> {
      try {
        databaseService.deleteTransaction(ID);
        transactionList.remove(position);
        // fake pause of 2 seconds to make process indicator visible for at least 2 seconds
        Thread.sleep(2000);
        runOnUiThread(() -> {
          toggleProcessIndicator(null);
          adapter.notifyItemRemoved(position);
          Toast.makeText(
                  TransactionsActivity.this,
                  getString(R.string.transaction_deleted),
                  Toast.LENGTH_SHORT
          ).show();
        });
      } catch(Exception exception) {
        runOnUiThread(() -> {
          toggleProcessIndicator(null);
          handleException(exception);
        });
      }

    });
  }
}