package com.borislavvucicevic.budgetmate.models;

import com.borislavvucicevic.budgetmate.enums.CacheKey;
import com.borislavvucicevic.budgetmate.enums.Month;
import com.borislavvucicevic.budgetmate.enums.ReportType;
import com.borislavvucicevic.budgetmate.enums.TransactionType;
import com.borislavvucicevic.budgetmate.services.CacheService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class DailyReport extends Report {
  public DailyReport(LocalDate date, List<Transaction> transactionList) {
    super(ReportType.DAILY, date, date, transactionList);
    prepare();
  }

  @Override
  protected void prepare() {
    calculateTotals();
  }

  @Override
  protected void calculateTotals() {
    for (Transaction transaction : transactionList) {
      String categoryName = CacheService
              .get(CacheKey.CATEGORIES, transaction.getCategoryID(),Category.class)
              .getName();

      TransactionType transactionType = transaction.getType();
      double amount = Math.round(transaction.getAmount() * 100.0) / 100.0;

      Double byTransactionTypeValue = totals.get(transactionType);

      if (byTransactionTypeValue == null) {
        byTransactionTypeValue = 0.0;
      }

      totals.put(transactionType, byTransactionTypeValue + amount);

      Double byCategoryValue = transactionType == TransactionType.INCOME ?
              totalIncomesByCategory.get(categoryName) :
              totalExpensesByCategory.get(categoryName);


      if (byCategoryValue == null) {
        byCategoryValue = 0.0;
      }

      if (transactionType == TransactionType.INCOME) {
        totalIncomesByCategory.put(categoryName, byCategoryValue + amount);
      } else {
        totalExpensesByCategory.put(categoryName, byCategoryValue + amount);
      }
    }
  }
}
