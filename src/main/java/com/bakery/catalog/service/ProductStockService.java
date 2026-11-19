package com.bakery.catalog.service;

import java.math.BigDecimal;
import java.util.List;

/** Stock changes join the caller's order transaction. */
public interface ProductStockService {

  List<StockSnapshot> deductForCheckout(List<StockRequest> requests);

  void restoreAfterCancellation(List<StockReturn> returns);

  record StockReturn(Long productId, int quantity) {}

  record StockRequest(Long productId, int quantity, BigDecimal expectedPrice) {}

  record StockSnapshot(Long productId, String productName, BigDecimal unitPrice,
      int quantity, int preparationDays) {}
}
