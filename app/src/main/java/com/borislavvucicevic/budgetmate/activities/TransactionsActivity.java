package com.borislavvucicevic.budgetmate.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.TemplateActivity;
import com.borislavvucicevic.budgetmate.models.Category;
import com.borislavvucicevic.budgetmate.models.Transaction;
import com.borislavvucicevic.budgetmate.adapters.TransactionCardAdapter;
import com.borislavvucicevic.budgetmate.models.TransactionPage;
import com.borislavvucicevic.budgetmate.enums.CacheKey;
import com.borislavvucicevic.budgetmate.services.CacheService;
import com.borislavvucicevic.budgetmate.services.LocalisationService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Activity responsible for displaying and managing the authenticated user's
 * transactions in the BudgetMate application.
 *
 * <p>This activity presents transactions in a {@link RecyclerView} using a
 * {@link TransactionCardAdapter}. Transactions are loaded from the database
 * in pages and appended to the displayed list as the user scrolls toward the
 * end of the currently loaded data.</p>
 *
 * <p>The activity supports creating and editing transactions through
 * {@link TransactionsUpsert}, deleting existing transactions after user
 * confirmation, and preserving pagination information through
 * {@link CacheService}.</p>
 *
 * <p>Database operations are performed asynchronously on background threads
 * to prevent blocking the Android UI thread. User-interface updates are
 * subsequently executed on the main thread.</p>
 *
 * <p>The activity also supports changing the application's locale through
 * the locale selector inherited from {@link TemplateActivity}.</p>
 *
 * @see TemplateActivity
 * @see Transaction
 * @see TransactionCardAdapter
 * @see TransactionPage
 * @see TransactionsUpsert
 * @see CacheService
 */
public class TransactionsActivity extends TemplateActivity {
  /**
   * Activity result launcher used to open {@link TransactionsUpsert} and
   * receive the transaction that was created or updated.
   *
   * <p>The returned result is processed by {@link #handleUpsertResult(ActivityResult)}.</p>
   */
  private final ActivityResultLauncher<Intent> transactionUpsertLauncher = registerForActivityResult(
          new ActivityResultContracts.StartActivityForResult(),
          this::handleUpsertResult
  );

  /**
   * List containing the transactions currently displayed by the activity.
   *
   * <p>The list is initially populated with transactions stored in
   * {@link CacheService}.</p>
   */
  private final ArrayList<Transaction> transactionList = new ArrayList<>(CacheService.readList(CacheKey.TRANSACTIONS));

  /**
   * Firestore document representing the last transaction retrieved during
   * the previous pagination request.
   *
   * <p>This value is used as the starting point when requesting the next
   * page of transactions.</p>
   */
  private DocumentSnapshot lastVisibleDocument = CacheService.read(CacheKey.LAST_VISIBLE_DOCUMENT, DocumentSnapshot.class);

  /**
   * Indicates whether additional transaction pages are available.
   *
   * <p>The initial value is restored from {@link CacheService}.</p>
   */
  private boolean hasNextPage = Boolean.TRUE.equals(CacheService.read(CacheKey.HAS_NEXT_PAGE, Boolean.class));

  /**
   * Indicates whether a transaction-loading operation is currently running.
   *
   * <p>This flag prevents multiple pagination requests from being executed
   * simultaneously.</p>
   */
  private boolean isLoadingTransactions = false;

  /**
   * Holds the number of loaded transactions from the last load. Used to properly notify the
   * recycler view about the changes.
   * */
  private int loadedTransactionsCount = 0;

  /**
   * Adapter responsible for displaying transaction cards inside the
   * {@link RecyclerView}.
   */
  private TransactionCardAdapter adapter;

  /**
   * RecyclerView used to display the user's transactions.
   */
  private RecyclerView recyclerView;

  /**
   * Floating action button used to open the transaction creation screen.
   */
  FloatingActionButton floatingActionButton;

  /**
   * Called when the transactions activity is first created.
   *
   * <p>This method enables edge-to-edge rendering and configures the activity
   * layout to respect system-bar insets.</p>
   *
   * <p>The cached transaction list is sorted by creation date in descending
   * order so that the newest transactions appear first. A
   * {@link TransactionCardAdapter} is then initialized and attached to the
   * {@link RecyclerView}.</p>
   *
   * <p>If no transactions are currently stored in the local list,
   * {@link #handleLoadTransactions()} is called to retrieve the first page from
   * the database.</p>
   *
   * @param savedInstanceState previously saved activity state, or
   *                           {@code null} if the activity is being created
   *                           for the first time
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
      Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
      return insets;
    });

    // sorting list
    transactionList.sort(Comparator.comparing(Transaction::getCreatedOn).reversed());

    // initializing adapter
    adapter = new TransactionCardAdapter(this, transactionList, this::showDeleteConfirmationDialog, this::openTransactionsUpsertActivity);

    // setting up the recycler view
    this.setUpRecyclerView();

    // load transactions
    if (transactionList.isEmpty()) {
      this.handleLoadTransactions();
    }
  }

  /**
   * Called when the activity's window becomes detached.
   *
   * <p>The current pagination state is stored in {@link CacheService} so that
   * the activity can continue loading transactions from the correct position
   * when it is opened again.</p>
   *
   * <p>The cached state includes whether another page exists and the last
   * Firestore document retrieved.</p>
   */
  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    CacheService.store(CacheKey.HAS_NEXT_PAGE, this.hasNextPage);
    CacheService.store(CacheKey.LAST_VISIBLE_DOCUMENT, this.lastVisibleDocument);
  }

  /**
   * Returns the layout resource used by this activity.
   *
   * @return resource identifier of the transactions activity layout
   */
  @Override
  protected int getLayoutID() {
    return R.layout.activity_transactions;
  }

  /**
   * Retrieves and stores references to the user-interface widgets contained
   * in the transactions activity layout.
   *
   * <p>This includes the transaction {@link RecyclerView}, process indicator,
   * process message, floating action button, and locale selector.</p>
   */
  @Override
  protected void grabWidgets() {
    super.grabWidgets();
    recyclerView = findViewById(R.id.recyclerView);
    floatingActionButton = findViewById(R.id.floatingActionButton);
  }

  /**
   * Registers event listeners for the interactive controls on the
   * transactions screen.
   *
   * <p>The floating action button opens the transaction creation screen,
   * while the locale selector uses the locale change handler provided by
   * {@link LocalisationService}.</p>
   */
  @Override
  protected void setListeners() {
    super.setListeners();
    floatingActionButton.setOnClickListener(v -> this.openTransactionsUpsertActivity());
  }

  /**
   * Configures the {@link RecyclerView} used to display transactions.
   *
   * <p>A {@link LinearLayoutManager} and the transaction adapter are attached
   * to the RecyclerView. A scroll listener is also registered to implement
   * incremental transaction loading.</p>
   *
   * <p>When the user scrolls downward and reaches approximately five items
   * from the end of the loaded list, another page is requested if additional
   * pages are available and no loading operation is already running.</p>
   */
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
          handleLoadTransactions();
        }
      }
    });
  }

  /**
   * Opens the transaction creation activity.
   *
   * <p>The activity is launched using {@link #transactionUpsertLauncher},
   * allowing the newly created transaction to be returned and inserted into
   * the currently displayed list.</p>
   */
  private void openTransactionsUpsertActivity() {
    Intent intent = new Intent(TransactionsActivity.this, TransactionsUpsert.class);
    transactionUpsertLauncher.launch(intent);
  }

  /**
   * Processes the result returned from the transaction creation or editing
   * activity.
   *
   * <p>If the operation completed successfully, the transaction stored under
   * {@link CacheKey#TRANSACTION_UPSERT_OBJECT} is retrieved from
   * {@link CacheService}. The method searches the existing list for a
   * transaction with the same identifier.</p>
   *
   * <p>If no matching transaction exists, the returned transaction is treated
   * as a newly created transaction and inserted at the beginning of the list.
   * If a match is found, the existing transaction is replaced and the
   * corresponding RecyclerView item is refreshed.</p>
   *
   * <p>Temporary cache values used by the upsert operation are cleared after
   * the result has been processed.</p>
   *
   * @param result result returned by {@link TransactionsUpsert}
   */
  @UiThread
  private void handleUpsertResult(@NonNull ActivityResult result) {
    if(result.getResultCode() == Activity.RESULT_OK) {
      Transaction transactionUpsertObject = CacheService.read(CacheKey.TRANSACTION_UPSERT_OBJECT, Transaction.class);
      int positionInView = IntStream.range(0, transactionList.size())
              .filter(i -> transactionList.get(i).equals(transactionUpsertObject))
              .findFirst()
              .orElse(-1);


      if(positionInView == -1) {
        transactionList.add(0, transactionUpsertObject);
        adapter.notifyItemInserted(0);
      } else {
        transactionList.set(positionInView, transactionUpsertObject);
        adapter.notifyItemChanged(positionInView);
      }

    }

    CacheService.clear(CacheKey.TRANSACTION_UPSERT_OBJECT);
  }

  /**
   * Toggles the visibility of the recycler view.
   */
  private void toggleRecyclerView() {
    recyclerView.setVisibility(
            recyclerView.getVisibility() != View.GONE ?
                    View.GONE :
                    View.VISIBLE
    );
  }

  /**
   * Starts loading the next page of transactions asynchronously
   * by executing {@link #loadTransactions()} on the background thread.
   *
   * <p>The operation is skipped if transactions are already being loaded or no
   * additional pages are available. On success, {@link #loadingSuccessful()} is
   * executed on the UI thread.</p>
   *
   * @see #doInBackground(Runnable, Runnable)
   * @see #loadTransactions()
   * @see #loadingSuccessful()
   */
  @UiThread
  private void handleLoadTransactions() {
    // Exit immediately if loading is in process, or there are no more transactions to load
    if(this.isLoadingTransactions || !this.hasNextPage) {
      return;
    }
    this.isLoadingTransactions = true;
    showToast(getString(R.string.transactions_loading));
    // fetch transaction in background
    this.doInBackground(this::loadTransactions, this::loadingSuccessful);
  }

  /**
   * Loads the next page of transactions and updates the pagination state.
   *
   * <p>Each transaction is assigned its cached category, then added to the
   * transaction list and stored in the cache.</p>
   * 
   * @apiNote Called on the background thread as a heavy 
   * task using {@link #doInBackground(Runnable, Runnable)}
   */
  @WorkerThread
  private void loadTransactions() {
    String uid = authService.getUserID();
    TransactionPage page = databaseService.getTransactions(
            uid,
            TransactionPage.PAGE_SIZE,
            this.lastVisibleDocument
    );
    this.hasNextPage = page.hasNextPage();
    this.lastVisibleDocument = page.getLastVisibleDocument();
    List<Transaction> loadedTransactions = page.getTransactionList();

    // fetching transaction categories
    for(Transaction transaction : loadedTransactions) {
      Category category = CacheService.get(CacheKey.CATEGORIES, transaction.getCategoryID(), Category.class);
      transaction.setCategory(
              category != null ?
                      category :
                      new Category(
                              transaction.getCategoryID(),
                              "No category",
                              transaction.getUserID()
                      )
      );
    }

    this.transactionList.addAll(loadedTransactions);
    this.loadedTransactionsCount = loadedTransactions.size();
    CacheService.store(CacheKey.TRANSACTIONS, loadedTransactions);
  }

  /**
   * Handles successful transaction loading on the UI thread.
   *
   * <p>Resets the loading state, shows a confirmation message, and notifies the
   * adapter about the newly inserted transactions.</p>
   * 
   * @apiNote Passed to {@code whenDone} parameter of the {@link #doInBackground(Runnable, Runnable)}
   */
  @UiThread
  private void loadingSuccessful() {
    isLoadingTransactions = false;
    showToast(getString(R.string.transactions_loaded));
    // index at which the newly loaded transactions begin
    int positionStart = transactionList.size() - loadedTransactionsCount;
    // refreshing data
    adapter.notifyItemRangeInserted(positionStart, loadedTransactionsCount);
  }

  /**
   * Displays a confirmation dialog before deleting a transaction.
   *
   * <p>The dialog asks the user to confirm the deletion. Selecting the
   * positive action calls {@link #handleDeleteTransaction()}, while
   * selecting the negative action dismisses the dialog without modifying
   * the transaction.</p>
   */
  private void showDeleteConfirmationDialog() {
    new MaterialAlertDialogBuilder(this, R.style.CustomAlertDialogTheme)
            .setTitle(R.string.delete_transaction_title)
            .setMessage(R.string.delete_transaction_message)
            .setPositiveButton(R.string.delete_transaction_yes, (d, which) -> handleDeleteTransaction())
            .setNegativeButton(R.string.delete_transaction_no, (d, which) -> d.dismiss())
            .setCancelable(true)
            .show();
  }

  /**
   * Deletes the specified transaction asynchronously.
   *
   * <p>On success, removes it from the local list, restores the transaction view,
   * shows a confirmation message, and notifies the adapter.</p>
   */
  @UiThread
  private void handleDeleteTransaction() {
    String ID = CacheService.read(CacheKey.TRANSACTION_TO_DELETE_ID, String.class);

    if(ID == null) {
      throw new IllegalArgumentException("For some reason, the ID of the chosen " +
              "transaction has not been stored in cache.");
    }

    int position = IntStream.range(0, transactionList.size())
            .filter(i -> transactionList.get(i).getID().equals(ID))
            .findFirst()
            .orElse(-1);

    this.toggleRecyclerView();
    this.doInBackground(
            // lambda function that actually deletes the transaction from database
            // represents the heavy task
            () -> {
              databaseService.deleteTransaction(ID);
              transactionList.remove(position);
            },
            // lambda function that updates the UI
            // represents an action done when heavy task completes successfully
            () -> {
              CacheService.clear(CacheKey.TRANSACTION_TO_DELETE_ID);
              toggleRecyclerView();
              showToast(getString(R.string.transaction_deleted));
              adapter.notifyItemRemoved(position);
            }
    );
  }
}