package com.borislavvucicevic.budgetmate.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.models.Category;
import com.borislavvucicevic.budgetmate.models.Transaction;
import com.borislavvucicevic.budgetmate.adapters.TransactionCardAdapter;
import com.borislavvucicevic.budgetmate.models.TransactionPage;
import com.borislavvucicevic.budgetmate.enums.CacheKey;
import com.borislavvucicevic.budgetmate.services.AuthService;
import com.borislavvucicevic.budgetmate.services.CacheService;
import com.borislavvucicevic.budgetmate.services.DatabaseService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;

public class TransactionsActivity extends AppCompatActivity {
  private final String TRANSACTION_ACTIVITY = "TRANSACTION_ACTIVITY";
  private final ActivityResultLauncher<Intent> transactionUpsertLauncher = registerForActivityResult(
          new ActivityResultContracts.StartActivityForResult(),
          this::handleUpsertResult
  );
  private final int PAGE_SIZE = 20;
  private final AuthService authService = new AuthService();
  private final DatabaseService databaseService = new DatabaseService();
  private final ArrayList<Transaction> transactionList = new ArrayList<Transaction>(CacheService.readList(CacheKey.TRANSACTIONS));
  private DocumentSnapshot lastVisibleDocument = CacheService.read(CacheKey.LAST_VISIBLE_DOCUMENT, DocumentSnapshot.class);
  private boolean hasNextPage = Boolean.TRUE.equals(CacheService.read(CacheKey.HAS_NEXT_PAGE, Boolean.class));
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

    // sorting list
    transactionList.sort(Comparator.comparing(Transaction::getCreatedOn).reversed());

    // grabbing widgets
    recyclerView = findViewById(R.id.recyclerView);
    processIndicator = findViewById(R.id.processIndicator);
    tvProcessMessage = findViewById(R.id.tvProcessMessage);
    FloatingActionButton floatingActionButton = findViewById(R.id.floatingActionButton);

    // setting listeners
    floatingActionButton.setOnClickListener(v -> this.openTransactionsUpsertActivity());

    // initializing adapter
    adapter = new TransactionCardAdapter(this, transactionList, this::showDeleteConfirmationDialog, this::openTransactionsUpsertActivity);

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
    CacheService.store(CacheKey.HAS_NEXT_PAGE, this.hasNextPage);
    CacheService.store(CacheKey.LAST_VISIBLE_DOCUMENT, this.lastVisibleDocument);
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
  private void openTransactionsUpsertActivity() {
    Intent intent = new Intent(TransactionsActivity.this, TransactionsUpsert.class);
    transactionUpsertLauncher.launch(intent);
  }

  private void handleUpsertResult(ActivityResult result) {
    if(result.getResultCode() == Activity.RESULT_OK) {
      Transaction transactionUpsertObject = CacheService.read(CacheKey.TRANSACTION_UPSERT_OBJECT, Transaction.class);
      int positionInView = -1;

      if(transactionUpsertObject != null) {
        for(int i = 0; i < transactionList.size(); i++) {
          Transaction transaction = transactionList.get(i);
          if(transactionUpsertObject.getID().equals(transaction.getID())) {
            positionInView = i;
            break;
          }
        }

        if(positionInView == -1) {
          transactionList.add(0, transactionUpsertObject);
          adapter.notifyItemInserted(0);
        } else {
          transactionList.set(positionInView, transactionUpsertObject);
          adapter.notifyItemChanged(positionInView);
        }
      }
    }

    CacheService.clear(CacheKey.TRANSACTION_UPSERT_OBJECT);
    CacheService.clear(CacheKey.POSITION_IN_VIEW);
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
          Category category = CacheService.get(CacheKey.CATEGORIES, transaction.getCategoryID(), Category.class);
          transaction.setCategory(category != null ? category : new Category(transaction.getUserID(), null, "No category"));
        }

        this.transactionList.addAll(loadedTransactions);
        CacheService.store(CacheKey.TRANSACTIONS, loadedTransactions);
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

  private void showDeleteConfirmationDialog(@NonNull String ID) {
    new MaterialAlertDialogBuilder(this, R.style.CustomAlertDialogTheme)
            .setTitle(R.string.delete_transaction_title)
            .setMessage(R.string.delete_transaction_message)
            .setPositiveButton(R.string.delete_transaction_yes, (d, which) -> handleDeleteTransaction(ID))
            .setNegativeButton(R.string.delete_transaction_no, (d, which) -> d.dismiss())
            .setCancelable(true)
            .show();
  }

  private void handleDeleteTransaction(@NonNull String ID) {
    int position = -1;

    for(int i = 0; i < transactionList.size(); i++) {
      Transaction transaction = transactionList.get(i);
      if(transaction.getID().equals(ID)) {
        position = i;
        break;
      }
    }

    this.toggleProcessIndicator(R.string.deleting_transaction);
    int finalPosition = position;
    Executors.newSingleThreadExecutor().execute(() -> {
      try {
        databaseService.deleteTransaction(ID);
        transactionList.remove(finalPosition);
        // fake pause of 2 seconds to make process indicator visible for at least 2 seconds
        Thread.sleep(2000);
        runOnUiThread(() -> {
          toggleProcessIndicator(null);
          adapter.notifyItemRemoved(finalPosition);
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