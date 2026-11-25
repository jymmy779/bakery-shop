package com.bakery.order.service.impl;

import com.bakery.catalog.service.ProductStockService;
import com.bakery.catalog.service.ProductStockService.StockReturn;
import com.bakery.order.entity.OrderStatus;
import com.bakery.order.entity.OrderStatusHistory;
import com.bakery.order.entity.PaymentStatus;
import com.bakery.order.repository.OrderDetailRepository;
import com.bakery.order.repository.OrderRepository;
import com.bakery.order.repository.OrderStatusHistoryRepository;
import com.bakery.order.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExpirationService {

  private static final int EXPIRATION_MINUTES = 5;

  private final OrderRepository orderRepository;
  private final PaymentRepository paymentRepository;
  private final OrderDetailRepository orderDetailRepository;
  private final OrderStatusHistoryRepository historyRepository;
  private final ProductStockService productStockService;

  /**
   * Quét và hủy tự động các đơn hàng PENDING quá 5 phút chưa thanh toán.
   * Chạy định kỳ mỗi 30 giây.
   */
  @Scheduled(fixedDelay = 30000)
  @Transactional
  public void cancelExpiredPendingOrders() {
    LocalDateTime threshold = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusMinutes(EXPIRATION_MINUTES);
    List<com.bakery.order.entity.Order> expiredOrders = orderRepository.findExpiredPendingOrders(OrderStatus.PENDING, threshold);

    if (expiredOrders.isEmpty()) {
      return;
    }

    log.info("Phát hiện {} đơn hàng quá hạn 5 phút chưa thanh toán. Tiến hành hủy tự động...", expiredOrders.size());

    for (var order : expiredOrders) {
      try {
        var paymentOpt = paymentRepository.findByOrder_Id(order.getId());
        if (paymentOpt.isPresent() && paymentOpt.get().getStatus() == PaymentStatus.PAID) {
          continue; // Đã thanh toán, không hủy
        }

        // 1. Hoàn tồn kho bánh
        var details = orderDetailRepository.findByOrder_IdOrderByIdAsc(order.getId());
        productStockService.restoreAfterCancellation(
            details.stream().map(d -> new StockReturn(d.getProduct().getId(), d.getQuantity())).toList()
        );

        // 2. Cập nhật trạng thái đơn sang CANCELLED
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason("Hết thời gian thanh toán (" + EXPIRATION_MINUTES + " phút)");
        order.setCancelledAt(now);
        orderRepository.save(order);

        // 3. Ghi lịch sử đơn hàng
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setFromStatus(OrderStatus.PENDING);
        history.setToStatus(OrderStatus.CANCELLED);
        history.setChangedBy(null); // Hệ thống tự động
        history.setChangedAt(now);
        history.setNote("Hệ thống tự động hủy do hết hạn thanh toán (" + EXPIRATION_MINUTES + " phút)");
        historyRepository.save(history);

        log.info("Đã tự động hủy đơn hàng quá hạn: {} (ID: {})", order.getOrderCode(), order.getId());
      } catch (Exception ex) {
        log.error("Lỗi khi hủy tự động đơn hàng ID: " + order.getId(), ex);
      }
    }
  }
}
