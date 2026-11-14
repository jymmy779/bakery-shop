package com.bakery.order.service.impl;

import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import com.bakery.account.service.AccountService;
import com.bakery.catalog.service.ProductStockService;
import com.bakery.catalog.service.ProductStockService.StockReturn;
import com.bakery.common.exception.ConflictException;
import com.bakery.common.exception.InvalidOperationException;
import com.bakery.common.exception.ResourceNotFoundException;
import com.bakery.order.dto.CancelOrderRequest;
import com.bakery.order.entity.OrderStatus;
import com.bakery.order.entity.OrderStatusHistory;
import com.bakery.order.entity.PaymentStatus;
import com.bakery.order.repository.CheckoutReferenceRepository;
import com.bakery.order.repository.OrderDetailRepository;
import com.bakery.order.repository.OrderRepository;
import com.bakery.order.repository.OrderStatusHistoryRepository;
import com.bakery.order.repository.PaymentRepository;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerOrderCancellationServiceImpl {

  private final AccountService accounts;
  private final OrderRepository orders;
  private final PaymentRepository payments;
  private final OrderDetailRepository details;
  private final OrderStatusHistoryRepository history;
  private final CheckoutReferenceRepository references;
  private final ProductStockService stock;
  private final Validator validator;

  @Transactional
  public void cancel(Long accountId, Long orderId, CancelOrderRequest request) {
    var customer = accounts.getById(accountId);
    if (customer.getRole() != Role.ROLE_CUSTOMER || customer.getStatus() != AccountStatus.ACTIVE) {
      throw new AccessDeniedException("Tài khoản không được phép hủy đơn hàng.");
    }
    if (request == null || !validator.validate(request).isEmpty()) {
      throw new InvalidOperationException("Vui lòng nhập lý do hủy đơn từ 1 đến 500 ký tự.");
    }
    // Locks the same order row as staff payment confirmation, before reading payment/details.
    var order = orders.findForCustomerCancellation(orderId, accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng của bạn."));
    if (order.getStatus() != OrderStatus.PENDING) {
      throw new ConflictException("Chỉ được hủy đơn đang chờ xử lý. Vui lòng kiểm tra trạng thái mới nhất hoặc liên hệ cửa hàng.");
    }
    var payment = payments.findByOrder_Id(orderId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin thanh toán của đơn hàng."));
    if (payment.getStatus() != PaymentStatus.PENDING) {
      throw new ConflictException("Đơn hàng đã ghi nhận thanh toán. Vui lòng liên hệ cửa hàng để được hỗ trợ.");
    }
    stock.restoreAfterCancellation(details.findByOrder_IdOrderByIdAsc(orderId).stream()
        .map(item -> new StockReturn(item.getProduct().getId(), item.getQuantity())).toList());
    var now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    order.setStatus(OrderStatus.CANCELLED);
    order.setCancellationReason(request.getReason().trim());
    order.setCancelledAt(now);
    var event = new OrderStatusHistory();
    event.setOrder(order);
    event.setFromStatus(OrderStatus.PENDING);
    event.setToStatus(OrderStatus.CANCELLED);
    event.setChangedBy(references.account(accountId));
    event.setChangedAt(now);
    event.setNote("Khách hàng hủy đơn: " + order.getCancellationReason());
    // The history note has a 500-character column; the full reason is stored on the order.
    if (event.getNote().length() > 500) event.setNote(order.getCancellationReason());
    history.saveAndFlush(event);
  }
}
