package com.bakery.order.service.impl;

import com.bakery.account.entity.Account;
import com.bakery.account.repository.AccountRepository;
import com.bakery.catalog.service.ProductStockService;
import com.bakery.catalog.service.ProductStockService.StockReturn;
import com.bakery.common.exception.InvalidOperationException;
import com.bakery.common.exception.ResourceNotFoundException;
import com.bakery.order.dto.OrderStatusUpdateRequest;
import com.bakery.order.entity.Order;
import com.bakery.order.entity.OrderStatus;
import com.bakery.order.entity.OrderStatusHistory;
import com.bakery.order.entity.Payment;
import com.bakery.order.entity.PaymentStatus;
import com.bakery.order.repository.OrderDetailRepository;
import com.bakery.order.repository.OrderRepository;
import com.bakery.order.repository.OrderStatusHistoryRepository;
import com.bakery.order.repository.PaymentRepository;
import com.bakery.order.service.AdminOrderStatusService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminOrderStatusServiceImpl implements AdminOrderStatusService {

  private final OrderRepository orders;
  private final OrderDetailRepository details;
  private final OrderStatusHistoryRepository history;
  private final PaymentRepository payments;
  private final AccountRepository accounts;
  private final PaymentStaffAccess staffAccess;
  private final ProductStockService stock;

  @Override
  @Transactional
  public void updateOrderStatus(Long staffId, Long orderId, OrderStatusUpdateRequest request) {
    staffAccess.requireActiveStaff(staffId);
    Account staff = accounts.findById(staffId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản nhân viên."));
    Order order = orders.findById(orderId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng."));

    OrderStatus currentStatus = order.getStatus();
    OrderStatus newStatus = request.getStatus();

    if (currentStatus == newStatus) {
      throw new InvalidOperationException("Đơn hàng hiện tại đã ở trạng thái này.");
    }

    if (currentStatus == OrderStatus.DELIVERED) {
      throw new InvalidOperationException("Đơn hàng đã giao thành công, không thể thay đổi trạng thái.");
    }

    if (currentStatus == OrderStatus.CANCELLED) {
      throw new InvalidOperationException("Đơn hàng đã bị hủy, không thể thay đổi trạng thái.");
    }

    // Kiểm tra chuyển đổi trạng thái hợp lệ
    // PENDING -> CONFIRMED, CANCELLED
    // CONFIRMED -> DELIVERING, CANCELLED
    // DELIVERING -> DELIVERED, CANCELLED
    if (currentStatus == OrderStatus.PENDING) {
      if (newStatus != OrderStatus.CONFIRMED && newStatus != OrderStatus.CANCELLED) {
        throw new InvalidOperationException("Đơn hàng chờ xử lý chỉ có thể chuyển sang Đã xác nhận hoặc Đã hủy.");
      }
    } else if (currentStatus == OrderStatus.CONFIRMED) {
      if (newStatus != OrderStatus.DELIVERING && newStatus != OrderStatus.CANCELLED) {
        throw new InvalidOperationException("Đơn hàng đã xác nhận chỉ có thể chuyển sang Đang giao hoặc Đã hủy.");
      }
    } else if (currentStatus == OrderStatus.DELIVERING) {
      if (newStatus != OrderStatus.DELIVERED && newStatus != OrderStatus.CANCELLED) {
        throw new InvalidOperationException("Đơn hàng đang giao chỉ có thể chuyển sang Đã giao hoặc Đã hủy.");
      }
    }

    // Nếu hủy đơn hàng, hoàn trả số lượng bánh về kho
    if (newStatus == OrderStatus.CANCELLED) {
      order.setCancellationReason(request.getNote() != null && !request.getNote().isBlank()
          ? request.getNote().trim() : "Quản trị viên / Nhân viên hủy đơn.");
      order.setCancelledAt(LocalDateTime.now());

      List<StockReturn> returnItems = details.findByOrder_IdOrderByIdAsc(orderId).stream()
          .map(d -> new StockReturn(d.getProduct().getId(), d.getQuantity()))
          .toList();
      stock.restoreAfterCancellation(returnItems);
    }

    order.setStatus(newStatus);
    orders.save(order);

    // Ghi lịch sử chuyển trạng thái
    OrderStatusHistory event = new OrderStatusHistory();
    event.setOrder(order);
    event.setFromStatus(currentStatus);
    event.setToStatus(newStatus);
    event.setChangedBy(staff);
    event.setChangedAt(LocalDateTime.now());
    event.setNote(request.getNote() != null && !request.getNote().isBlank() ? request.getNote().trim() : null);
    history.save(event);
  }
}
