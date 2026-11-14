package com.bakery.order.service.impl;

import com.bakery.common.exception.ResourceNotFoundException;
import com.bakery.order.dto.AdminOrderDetailResponse;
import com.bakery.order.dto.OrderStatusHistoryResponse;
import com.bakery.order.dto.OrderSummaryResponse;
import com.bakery.order.entity.OrderStatus;
import com.bakery.order.entity.PaymentMethod;
import com.bakery.order.entity.PaymentStatus;
import com.bakery.order.repository.OrderDetailRepository;
import com.bakery.order.repository.OrderRepository;
import com.bakery.order.repository.OrderStatusHistoryRepository;
import com.bakery.order.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderQueryServiceImpl {

  private final PaymentStaffAccess staffAccess;
  private final OrderRepository orders;
  private final OrderDetailRepository details;
  private final PaymentRepository payments;
  private final OrderStatusHistoryRepository history;
  private final CheckoutOrderMapper mapper;

  public Page<OrderSummaryResponse> findOrders(Long staffId, String search, OrderStatus status,
      PaymentStatus paymentStatus, int page) {
    staffAccess.requireActiveStaff(staffId);
    String term = search == null ? "" : search.trim();
    var sort = Sort.by(Sort.Direction.DESC, "createdAt", "id");
    var found = orders.findAdminOrders(term, status, paymentStatus,
        PageRequest.of(Math.max(0, page), 12, sort));
    int lastPage = Math.max(0, found.getTotalPages() - 1);
    if (found.getNumber() > lastPage) {
      found = orders.findAdminOrders(term, status, paymentStatus, PageRequest.of(lastPage, 12, sort));
    }
    return found.map(row -> {
      var result = new OrderSummaryResponse();
      result.setId(row.getId());
      result.setOrderCode(row.getOrderCode());
      result.setCustomerName(row.getCustomerName());
      result.setTotalAmount(row.getTotalAmount());
      result.setStatus(row.getStatus());
      result.setPaymentStatus(row.getPaymentStatus());
      result.setCreatedAt(row.getCreatedAt());
      result.setDeliveryDate(row.getDeliveryDate());
      return result;
    });
  }

  public AdminOrderDetailResponse getOrder(Long staffId, Long orderId) {
    staffAccess.requireActiveStaff(staffId);
    var entity = orders.findById(orderId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng."));
    var payment = payments.findByOrder_Id(orderId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thanh toán của đơn hàng."));
    var order = mapper.map(entity, details.findByOrder_IdOrderByIdAsc(orderId), payment);
    order.setStatusHistory(history.findByOrder_IdOrderByChangedAtAscIdAsc(orderId).stream().map(event -> {
      var entry = new OrderStatusHistoryResponse();
      entry.setFromStatus(event.getFromStatus());
      entry.setToStatus(event.getToStatus());
      entry.setChangedAt(event.getChangedAt());
      entry.setChangedByName(event.getChangedBy() == null ? null : event.getChangedBy().getFullName());
      entry.setNote(event.getNote());
      return entry;
    }).toList());
    return new AdminOrderDetailResponse(order, entity.getStatus() == OrderStatus.PENDING
        && payment.getStatus() == PaymentStatus.PENDING && payment.getMethod() == PaymentMethod.BANK_TRANSFER);
  }
}
