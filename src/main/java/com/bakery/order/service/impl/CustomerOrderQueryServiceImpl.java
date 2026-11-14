package com.bakery.order.service.impl;

import com.bakery.order.dto.OrderDetailResponse;
import com.bakery.order.dto.OrderStatusHistoryResponse;
import com.bakery.order.dto.OrderSummaryResponse;
import com.bakery.order.entity.OrderStatus;
import com.bakery.order.repository.OrderRepository;
import com.bakery.order.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Customer read use cases; order mutations and CMS workflows are implemented separately. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerOrderQueryServiceImpl {

  private static final int PAGE_SIZE = 6;
  private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");
  private final OrderRepository orders;
  private final OrderStatusHistoryRepository history;
  private final CheckoutResultServiceImpl results;

  public Page<OrderSummaryResponse> findCustomerOrders(Long accountId, OrderStatus status, int page) {
    var found = orders.findCustomerOrders(accountId, status,
        PageRequest.of(Math.max(0, page), PAGE_SIZE, NEWEST_FIRST));
    // Old bookmarks still show an existing page after the number of orders changes.
    int lastPage = Math.max(0, found.getTotalPages() - 1);
    if (found.getNumber() > lastPage) {
      found = orders.findCustomerOrders(accountId, status,
          PageRequest.of(lastPage, PAGE_SIZE, NEWEST_FIRST));
    }
    return found.map(row -> {
      var summary = new OrderSummaryResponse();
      summary.setId(row.getId());
      summary.setOrderCode(row.getOrderCode());
      summary.setCustomerName(row.getCustomerName());
      summary.setTotalAmount(row.getTotalAmount());
      summary.setStatus(row.getStatus());
      summary.setPaymentStatus(row.getPaymentStatus());
      summary.setDeliveryDate(row.getDeliveryDate());
      summary.setCreatedAt(row.getCreatedAt());
      return summary;
    });
  }

  public OrderDetailResponse getCustomerOrder(Long accountId, Long orderId) {
    // Check ownership before reading any line items, payment or status history.
    var order = results.getForCustomer(accountId, orderId);
    order.setStatusHistory(history.findByOrder_IdOrderByChangedAtAscIdAsc(orderId).stream()
        .map(event -> {
          var entry = new OrderStatusHistoryResponse();
          entry.setFromStatus(event.getFromStatus());
          entry.setToStatus(event.getToStatus());
          entry.setChangedAt(event.getChangedAt());
          entry.setChangedByName(event.getChangedBy() == null ? null : event.getChangedBy().getFullName());
          entry.setNote(event.getNote());
          return entry;
        }).toList());
    return order;
  }
}
