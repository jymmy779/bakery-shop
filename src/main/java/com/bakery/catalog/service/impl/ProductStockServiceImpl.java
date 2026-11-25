package com.bakery.catalog.service.impl;

import com.bakery.catalog.repository.ProductRepository;
import com.bakery.catalog.service.ProductStockService;
import com.bakery.common.exception.ConflictException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductStockServiceImpl implements ProductStockService {

  private final ProductRepository products;

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void restoreAfterCancellation(List<StockReturn> returns) {
    if (returns == null || returns.isEmpty()) {
      throw new ConflictException("Đơn hàng không có chi tiết bánh hợp lệ để hoàn kho.");
    }
    var quantities = new TreeMap<Long, Long>();
    for (var item : returns) {
      if (item == null || item.productId() == null || item.productId() <= 0 || item.quantity() <= 0) {
        throw new ConflictException("Chi tiết bánh hoặc số lượng hoàn kho không hợp lệ.");
      }
      long total = quantities.merge(item.productId(), (long) item.quantity(), Long::sum);
      if (total > Integer.MAX_VALUE) throw new ConflictException("Số lượng hoàn kho vượt giới hạn.");
    }
    // Same ascending product order as checkout. STOPPED products must also receive their stock back.
    // Do not clear the persistence context: the caller still owns the locked, managed order.
    for (var entry : quantities.entrySet()) {
      int quantity = entry.getValue().intValue();
      if (products.restoreCancelledStock(entry.getKey(), quantity, Integer.MAX_VALUE - quantity) != 1) {
        throw new ConflictException("Không thể hoàn tồn kho bánh. Đơn hàng chưa được hủy; vui lòng liên hệ cửa hàng.");
      }
    }
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public List<StockSnapshot> deductForCheckout(List<StockRequest> requests) {
    if (requests == null || requests.isEmpty()) throw new ConflictException("Giỏ bánh đang trống.");
    var ids = new HashSet<Long>();
    for (var request : requests) {
      if (request == null || request.productId() == null || request.productId() <= 0
          || request.quantity() < 1 || request.expectedPrice() == null
          || request.expectedPrice().signum() < 0 || !ids.add(request.productId())) {
        throw new ConflictException("Thông tin bánh hoặc số lượng không hợp lệ.");
      }
    }
    var snapshots = new ArrayList<StockSnapshot>();
    // Stable row order limits deadlocks when different baskets contain the same products.
    for (var request : requests.stream().sorted(Comparator.comparing(StockRequest::productId)).toList()) {
      if (products.deductCheckoutStock(request.productId(), request.quantity(), request.expectedPrice()) != 1) {
        throw new ConflictException("Giá, trạng thái bán hoặc tồn kho bánh đã thay đổi. Vui lòng kiểm tra lại giỏ bánh.");
      }
      // The conditional update clears stale managed entities and holds the updated row until commit.
      var product = products.findById(request.productId())
          .orElseThrow(() -> new ConflictException("Bánh không còn trong danh mục."));
      snapshots.add(new StockSnapshot(product.getId(), product.getName(), product.getPrice(),
          request.quantity(), product.getPreparationDays()));
    }
    return List.copyOf(snapshots);
  }
}
