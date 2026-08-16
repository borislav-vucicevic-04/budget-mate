package com.borislavvucicevic.budgetmate.adapters;

import android.content.Context;
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
import com.google.firebase.Timestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

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
 *
 * <p>The adapter also provides callback mechanisms for deleting and editing
 * transactions. Delete operations are delegated through
 * DeleteTransactionHandler, while edit operations store the selected
 * transaction in {@link CacheService} before invoking the supplied activity
 * navigation callback.</p>
 *
 * @see RecyclerView.Adapter
 * @see Transaction
 * @see UserProfile
 * @see CacheService
 */
public class TransactionCardAdapter extends RecyclerView.Adapter<TransactionCardAdapter.ViewHolder> {
  /** Context used to inflate layouts and access application resources. */
  private final Context context;

  /** Transactions displayed by this adapter. */
  private final ArrayList<Transaction> transactions;

  /**
   * Callback invoked when the user selects the delete action on a transaction
   * card.
   */
  private final Runnable deleteTransactionHandler;

  /**
   * Callback used to open the transaction creation/update activity after the
   * selected transaction has been stored in {@link CacheService}.
   */
  private final Runnable openTransactionsUpsertActivity;

  /**
   * Creates a new transaction card adapter.
   *
   * <p>The supplied transaction collection is copied into a new
   * {@link ArrayList}, which becomes the internal data set used by this
   * adapter.</p>
   *
   * @param context the context used to inflate views and access resources
   * @param transactions the transactions to display in the RecyclerView
   * @param deleteTransactionHandler callback invoked when a transaction
   *                                 should be deleted
   * @param openTransactionsUpsertActivity callback used to open the
   *                                       transaction editing screen
   */
  public TransactionCardAdapter(
          Context context,
          ArrayList<Transaction> transactions,
          Runnable deleteTransactionHandler,
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
   * <p>The {@code transaction_card} layout is inflated using the adapter's
   * context and wrapped inside a new {@link ViewHolder} instance.</p>
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
   * <p>The transaction details are displayed through
   * {@link ViewHolder#setDetails(Transaction)}. Delete and edit actions
   * are also bound to the corresponding buttons for the selected
   * transaction.</p>
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
    holder.setDetails(transaction);
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
   *
   * <p>It also binds the delete and edit buttons to callbacks supplied by
   * {@link TransactionCardAdapter}.</p>
   */
  public static class ViewHolder extends RecyclerView.ViewHolder {

    /** Context used to access strings, colors, and other resources. */
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

    /**
     * Button used to request deletion of the transaction represented by this
     * card.
     */
    private final Button btnDeleteTransaction;

    /**
     * Button used to open the selected transaction for editing.
     */
    private final Button btnEditTransaction;

    /** Container used to display the transaction notes section. */
    private final MaterialCardView cardTransactionNotes;

    /** Container used to display the transaction modification date. */
    private final LinearLayout layoutModifiedOn;

    /**
     * Creates a ViewHolder and obtains references to the views in the
     * transaction card layout.
     *
     * <p>All UI components required to display transaction information and
     * provide delete/edit actions are resolved from the supplied root
     * {@code itemView}.</p>
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
     * <p>The current {@link UserProfile} is retrieved from
     * {@link CacheService} so that the transaction amount can be displayed
     * using the user's configured home currency. If the profile cannot be
     * found, the method logs the condition and returns without updating the
     * card.</p>
     *
     * <p>The amount is prefixed with a plus sign for income or a minus sign
     * for expenses. Its text colur is also changed according to the
     * transaction type.</p>
     *
     * <p>Creation and modification timestamps are formatted using the
     * {@code dd.MM.yyyy hh:mm:ss} pattern. The date and time are displayed on
     * separate lines.</p>
     *
     * <p>The modification-date section is displayed only when the transaction
     * has been modified. The notes section is displayed only when notes are
     * available.</p>
     *
     * @param transaction the transaction whose details should be displayed
     */
    private void setDetails(Transaction transaction) {
      // Getting the user profile from the cache.
      UserProfile userProfile = CacheService.read(CacheKey.USER_PROFILE, UserProfile.class);

      if (userProfile == null) {
        Log.d("TRANSACTION_CARD_ADAPTER", "Somehow user profile is null!!!!!!");
        return;
      }

      // Formatter used for creation and modification dates.

      // Parsing values.
      String id = transaction.getID();

      String type =
              transaction.getType() == TransactionType.EXPENSE
                      ? context.getString(R.string.expense)
                      : context.getString(R.string.income);

      String amount =
              (transaction.getType() == TransactionType.INCOME ? "+" : "-")
                      + String.format(Locale.US, "%.2f", transaction.getAmount())
                      + " "
                      + CurrencyCode.parse(userProfile.getHomeCurrency());

      String categoryAndType =
              transaction.getCategory().getName() + " – " + type;

      // Selecting the amount color based on the transaction type.
      int amountTextColor =
              transaction.getType() == TransactionType.EXPENSE
                      ? context.getColor(R.color.red)
                      : context.getColor(R.color.green);

      // Displaying values.
      tvTransactionId.setText(id);
      tvTransactionCategoryAndType.setText(categoryAndType);
      tvTransactionAmount.setText(amount);
      tvTransactionCreatedOn.setText(this.formatTimestamp(
              transaction.getCreatedOn()
      ));

      tvTransactionAmount.setTextColor(amountTextColor);

      // Displaying modification information when available.
      if (transaction.getModifiedOn() != null) {
        tvTransactionModifiedOn.setText(this.formatTimestamp(
                transaction.getModifiedOn()
        ));

        layoutModifiedOn.setVisibility(View.VISIBLE);
      } else {
        layoutModifiedOn.setVisibility(View.GONE);
      }

      // Displaying transaction notes when available.
      if (transaction.getNotes() != null) {
        tvTransactionNotes.setText(transaction.getNotes());
        cardTransactionNotes.setVisibility(View.VISIBLE);
      } else {
        cardTransactionNotes.setVisibility(View.GONE);
      }
    }

    /**
     * Formats a timestamp to a local date string using pattern dd.MM.yyyy.
     *
     * @param timestamp a timestamp to be formatted.
     * @return a string representing given timestamp as a local date in format dd.MM.yyyy.
     * */
    @NonNull
    private String formatTimestamp(@NonNull Timestamp timestamp) {
      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
      Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanoseconds());

      // Convert to LocalDateTime using the device's current timezone
      LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

      return localDateTime.format(dateTimeFormatter);
    }

    /**
     * Binds the delete button to the supplied transaction deletion handler.
     *
     * <p>When the delete button is clicked, the transaction identifier is
     * forwarded to  DeleteTransactionHandler#onDelete(String). The
     * actual deletion operation is therefore handled outside the adapter.</p>
     *
     * @param deleteTransactionHandler callback responsible for handling the
     *                                 transaction deletion request
     * @param ID unique identifier of the transaction represented by this card
     */
    private void bindDeleteTransactionMethod(Runnable deleteTransactionHandler, String ID) {
      btnDeleteTransaction.setOnClickListener((v) -> {
        CacheService.store(CacheKey.TRANSACTION_TO_DELETE_ID, ID);
        deleteTransactionHandler.run();
      });
    }

    /**
     * Binds the edit button to the supplied transaction editing action.
     *
     * <p>When the edit button is clicked, the selected transaction is first
     * stored in {@link CacheService} under
     * {@link CacheKey#TRANSACTION_UPSERT_OBJECT}. The supplied
     * {@link Runnable} is then executed to open the activity responsible for
     * editing the transaction.</p>
     *
     * @param openTransactionsUpsertActivity callback that opens the transaction
     *                                       creation/update activity
     * @param transaction transaction represented by this card
     */
    private void bindOpenTransactionUpsertActivityMethod(Runnable openTransactionsUpsertActivity, Transaction transaction) {
      btnEditTransaction.setOnClickListener((v) -> {
        CacheService.store(CacheKey.TRANSACTION_UPSERT_OBJECT, transaction);
        openTransactionsUpsertActivity.run();
      });
    }
  }
}