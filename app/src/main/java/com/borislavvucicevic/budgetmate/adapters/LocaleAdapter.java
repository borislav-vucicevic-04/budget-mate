package com.borislavvucicevic.budgetmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.borislavvucicevic.budgetmate.R;
import com.borislavvucicevic.budgetmate.options.LocaleOption;

import java.util.List;

/**
 * Adapter used to display {@link LocaleOption} objects in a locale-selection
 * component, such as a {@link android.widget.Spinner}.
 *
 * <p>Each item is displayed using the {@code locale_switch_item} layout and
 * contains:
 *
 * <ul>
 *   <li>an icon representing the locale, and</li>
 *   <li>the user-visible title of the locale.</li>
 * </ul>
 *
 * <p>The same layout is used for both the currently selected item and the
 * items displayed in the drop-down list.
 */
public class LocaleAdapter extends ArrayAdapter<LocaleOption> {

  /**
   * Creates a new adapter for the supplied locale options.
   *
   * @param context the context used to access resources and inflate item layouts
   * @param items   the list of locale options displayed by the adapter
   */
  public LocaleAdapter(@NonNull Context context, @NonNull List<LocaleOption> items) {
    super(context, 0, items);
  }

  /**
   * Returns the view used to display the currently selected locale option.
   *
   * @param position    the position of the item within the adapter
   * @param convertView an existing view that can be reused, or {@code null}
   *                    if a new view must be created
   * @param parent      the parent view that the returned view will be attached to
   * @return the initialized view representing the locale option
   */
  @NonNull
  @Override
  public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
    return initView(position, convertView, parent);
  }

  /**
   * Returns the view used to display a locale option inside the drop-down list.
   *
   * @param position    the position of the item within the adapter
   * @param convertView an existing view that can be reused, or {@code null}
   *                    if a new view must be created
   * @param parent      the parent view that the returned view will be attached to
   * @return the initialized drop-down view representing the locale option
   */
  @NonNull
  @Override
  public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
    return initView(position, convertView, parent);
  }

  /**
   * Creates or reuses an item view and binds the locale option at the specified
   * position to it.
   *
   * <p>If {@code convertView} is {@code null}, a new view is inflated from
   * {@code R.layout.locale_switch_item}. The locale icon and title are then
   * assigned to the corresponding {@link ImageView} and {@link TextView}.
   *
   * @param position    the position of the locale option within the adapter
   * @param convertView an existing reusable view, or {@code null}
   * @param parent      the parent view used when inflating a new item layout
   * @return the initialized view representing the locale option
   */
  @NonNull
  private View initView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
    if (convertView == null) {
      convertView = LayoutInflater.from(getContext()).inflate(
              R.layout.locale_switch_item,
              parent,
              false
      );
    }

    ImageView imageViewIcon = convertView.findViewById(R.id.itemIcon);
    TextView textViewName = convertView.findViewById(R.id.itemText);

    LocaleOption currentItem = getItem(position);

    if (currentItem != null) {
      imageViewIcon.setImageResource(currentItem.getIconResId());
      textViewName.setText(currentItem.getTitle());
    }

    return convertView;
  }
}