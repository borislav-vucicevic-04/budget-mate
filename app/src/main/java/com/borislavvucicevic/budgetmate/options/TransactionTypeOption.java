package com.borislavvucicevic.budgetmate.options;

import androidx.annotation.NonNull;

import com.borislavvucicevic.budgetmate.enums.TransactionType;

/**
 * Represents a selectable transaction type option with a user-friendly display name.
 *
 * <p>This class is useful for displaying {@link TransactionType} values in UI components
 * such as spinners, dropdown menus, or selection lists.</p>
 *
 * <p>The {@link #toString()} method returns the display name, allowing instances of this
 * class to be shown directly in Android UI components.</p>
 */
public class TransactionTypeOption {

  /**
   * The user-friendly name displayed in the interface.
   */
  private final String displayName;

  /**
   * The transaction type represented by this option.
   */
  private final TransactionType type;

  /**
   * Creates a new transaction type option.
   *
   * @param displayName the user-friendly name of the transaction type
   * @param value the {@link TransactionType} represented by this option
   */
  public TransactionTypeOption(String displayName, TransactionType value) {
    this.displayName = displayName;
    this.type = value;
  }

  /**
   * Returns the user-friendly display name of this option.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Returns the transaction type represented by this option.
   *
   * @return the associated {@link TransactionType}
   */
  public TransactionType getType() {
    return type;
  }

  public String getTypeAsString() {
    if (type != null) return type.toString();
    else return "";
  }


  /**
   * Returns the display name of this transaction type option.
   *
   * <p>This type is used when the object is displayed in UI components such as
   * spinners or dropdown lists.</p>
   *
   * @return the non-null display name
   */
  @NonNull
  @Override
  public String toString() {
    return this.getDisplayName();
  }
}