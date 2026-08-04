package com.borislavvucicevic.budgetmate.services;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility service providing generic methods to sort maps by keys or values.
 * <p>
 * This class is final and cannot be instantiated.
 * </p>
 */
public final class MapSorterService {
  /**
   * Private constructor to prevent instantiation of this utility class.
   *
   * @throws UnsupportedOperationException if instantiation is attempted
   */
  private MapSorterService() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  /**
   * Sorts the provided map by its values in descending order.
   * <p>
   * The values of the map must implement the {@link Comparable} interface.
   * If the provided map is empty, the method immediately returns a new empty map.
   * </p>
   *
   * @param <K> the type of keys maintained by this map
   * @param <V> the type of mapped values, which must be comparable
   * @param map the map to be sorted, must not be null
   * @return a new {@link LinkedHashMap} sorted by its values
   */
  public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(@NotNull Map<K, V> map) {
    if (map.isEmpty()) {
      return new LinkedHashMap<>();
    }

    return map.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (oldValue, newValue) -> oldValue,
                    LinkedHashMap::new
            ));
  }
}
