package com.borislavvucicevic.budgetmate.adapters;

import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.enums.CacheKey;
import com.borislavvucicevic.budgetmate.enums.CurrencyCode;
import com.borislavvucicevic.budgetmate.enums.TransactionType;
import com.borislavvucicevic.budgetmate.models.Transaction;
import com.borislavvucicevic.budgetmate.models.UserProfile;
import com.borislavvucicevic.budgetmate.services.CacheService;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;

/**
 * A {@link RecyclerView.Adapter} responsible for displaying a list of
 * {@link Transaction} objects as transaction cards.
 *
 * <p>Each transaction card displays information such as:</p>
 * <ul>
 *   <li>Transaction position and ID</li>
 *   <li>Category and transaction type</li>
 *   <li>Transaction amount and currency</li>
 *   <li>Creation and modification dates</li>
 *   <li>Optional transaction notes</li>
 * </ul>
 *
 * <p>The transaction amount is displayed in green for income transactions
 * and red for expense transactions. The user's home currency is obtained
 * from the cached {@link UserProfile}.</p>
 */
public class TransactionCardAdapter extends RecyclerView.Adapter<TransactionCardAdapter.ViewHolder> {
  @FunctionalInterface
  public interface DeleteTransactionHandler {
    void onDelete(@NonNull String id);
  }

  /** Context used to inflate layouts and access application resources. */
  private final Context context;

  /** Transactions displayed by this adapter. */
  private final ArrayList<Transaction> transactions;

  private final DeleteTransactionHandler deleteTransactionHandler;

  private final Runnable openTransactionsUpsertActivity;

  /**
   * Creates a new transaction card adapter.
   *
   * @param context the context used to inflate views and access resources
   * @param transactions the transactions to display in the RecyclerView
   */
  public TransactionCardAdapter(
          Context context,
          ArrayList<Transaction> transactions,
          DeleteTransactionHandler deleteTransactionHandler,
          Runnable openTransactionsUpsertActivity
  ) {
    this.context = context;
    this.transactions = transactions;
    this.deleteTransactionHandler = deleteTransactionHandler;
    this.openTransactionsUpsertActivity = openTransactionsUpsertActivity;
  }

  /**
   * Creates a new {@link ViewHolder} by inflating the transaction card layout.
   *
   * @param parent the parent view group into which the new view will be added
   * @param viewType the view type of the new view
   * @return a new {@link ViewHolder} containing the inflated transaction card
   */
  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(
          @NonNull ViewGroup parent,
          int viewType
  ) {
    View view = LayoutInflater.from(context)
            .inflate(R.layout.transaction_card, parent, false);

    return new ViewHolder(view, context);
  }

  /**
   * Binds the transaction at the specified position to the supplied
   * {@link ViewHolder}.
   *
   * @param holder the ViewHolder that should display the transaction
   * @param position the position of the transaction in the adapter
   */
  @Override
  public void onBindViewHolder(
          @NonNull ViewHolder holder,
          int position
  ) {
    Transaction transaction = transactions.get(position);
    holder.setDetails(transaction, position);
    holder.bindDeleteTransactionMethod(deleteTransactionHandler, transaction.getID());
    holder.bindOpenTransactionUpsertActivityMethod(openTransactionsUpsertActivity, transaction);
  }

  /**
   * Returns the total number of transactions displayed by the adapter.
   *
   * @return the number of transactions in the data set
   */
  @Override
  public int getItemCount() {
    return transactions.size();
  }

  /**
   * Holds and manages the views belonging to a single transaction card.
   *
   * <p>The ViewHolder formats and displays transaction information and
   * conditionally shows the notes and modification-date sections when the
   * corresponding data is available.</p>
   */
  static class ViewHolder extends RecyclerView.ViewHolder {
    /** Context used to access strings, colours, and other resources. */
    private final Context context;


    /** Displays the transaction's position and unique ID. */
    private final TextView tvTransactionId;

    /** Displays the transaction category and type. */
    private final TextView tvTransactionCategoryAndType;

    /** Displays the formatted transaction amount and currency. */
    private final TextView tvTransactionAmount;

    /** Displays optional notes associated with the transaction. */
    private final TextView tvTransactionNotes;

    /** Displays the date on which the transaction was created. */
    private final TextView tvTransactionCreatedOn;

    /** Displays the date on which the transaction was last modified. */
    private final TextView tvTransactionModifiedOn;

    private final Button btnDeleteTransaction;

    private final Button btnEditTransaction;

    /** Container used to display the transaction notes section. */
    private final MaterialCardView cardTransactionNotes;

    /** Container used to display the transaction modification date. */
    private final LinearLayout layoutModifiedOn;

    /**
     * Creates a ViewHolder and obtains references to the views in the
     * transaction card layout.
     *
     * @param itemView the root view of the transaction card
     * @param context the context used to access resources
     */
    public ViewHolder(@NonNull View itemView, Context context) {
      super(itemView);

      this.context = context;

      tvTransactionId =
              itemView.findViewById(R.id.tvTransactionId);
      tvTransactionCategoryAndType =
              itemView.findViewById(R.id.tvTransactionCategoryAndType);
      tvTransactionAmount =
              itemView.findViewById(R.id.tvTransactionAmount);
      tvTransactionNotes =
              itemView.findViewById(R.id.tvTransactionNotes);
      tvTransactionCreatedOn =
              itemView.findViewById(R.id.tvTransactionCreatedOn);
      tvTransactionModifiedOn =
              itemView.findViewById(R.id.tvTransactionModifiedOn);
      btnDeleteTransaction =
              itemView.findViewById(R.id.btnDeleteTransaction);
      btnEditTransaction =
              itemView.findViewById(R.id.btnEditTransaction);
      cardTransactionNotes =
              itemView.findViewById(R.id.cardTransactionNotes);
      layoutModifiedOn =
              itemView.findViewById(R.id.layoutModifiedOn);
    }

    /**
     * Formats and displays the details of the supplied transaction.
     *
     * <p>The amount is prefixed with a plus sign for income or a minus sign
     * for expenses. Its text colour is also changed according to the
     * transaction type.</p>
     *
     * <p>The modification-date section is displayed only when the transaction
     * has been modified. The notes section is displayed only when notes are
     * available.</p>
     *
     * @param transaction the transaction whose details should be displayed
     * @param position the transaction's position in the RecyclerView
     */
    private void setDetails(Transaction transaction, int position) {
      // Getting the user profile from the cache.
      UserProfile userProfile = CacheService.read(CacheKey.USER_PROFILE, UserProfile.class);

      if (userProfile == null) {
        Log.d("TRANSACTION_CARD_ADAPTER", "Somehow user profile is null!!!!!!");
        return;
      }

      // Formatter used for creation and modification dates.
      SimpleDateFormat dateTimeFormatter =
              new SimpleDateFormat("dd.MM.yyyy hh:mm:ss");

      // Parsing values.
      String id = transaction.getID();

      String type =
              transaction.getType() == TransactionType.EXPENSE
                      ? context.getString(R.string.expense)
                      : context.getString(R.string.income);

      String amount =
              (transaction.getType() == TransactionType.INCOME ? "+" : "-")
                      + String.format("%.2f", transaction.getAmount())
                      + " "
                      + CurrencyCode.parse(userProfile.getHomeCurrency());

      String categoryAndType =
              transaction.getCategory().getName() + " – " + type;

      // Selecting the amount colour based on the transaction type.
      int amountTextColor =
              transaction.getType() == TransactionType.EXPENSE
                      ? context.getColor(R.color.red)
                      : context.getColor(R.color.green);

      // Displaying values.
      tvTransactionId.setText(id);
      tvTransactionCategoryAndType.setText(categoryAndType);
      tvTransactionAmount.setText(amount);
      tvTransactionCreatedOn.setText(
              dateTimeFormatter.format(
                      transaction.getCreatedOn().toDate()
              ).replace(" ", "\n")
      );

      tvTransactionAmount.setTextColor(amountTextColor);

      // Displaying modification information when available.
      if (transaction.getModifiedOn() != null) {
        tvTransactionModifiedOn.setText(
                dateTimeFormatter.format(
                        transaction.getModifiedOn().toDate()
                ).replace(" ", "\n")
        );

        layoutModifiedOn.setVisibility(View.VISIBLE);
      }

      // Displaying transaction notes when available.
      if (transaction.getNotes() != null) {
        tvTransactionNotes.setText(transaction.getNotes());
        cardTransactionNotes.setVisibility(View.VISIBLE);
      } else {
        cardTransactionNotes.setVisibility(View.GONE);
      }
    }
    private void bindDeleteTransactionMethod(DeleteTransactionHandler deleteTransactionHandler, String ID) {
      btnDeleteTransaction.setOnClickListener((v) -> deleteTransactionHandler.onDelete(ID));
    }
    private void bindOpenTransactionUpsertActivityMethod(Runnable openTransactionsUpsertActivity, Transaction transaction) {
      btnEditTransaction.setOnClickListener((v) -> {
        CacheService.store(CacheKey.TRANSACTION_UPSERT_OBJECT, transaction);
        openTransactionsUpsertActivity.run();
      });
    }
  }
}
