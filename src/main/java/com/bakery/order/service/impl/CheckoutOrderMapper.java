package com.bakery.order.service.impl;

import com.bakery.catalog.repository.ProductImageRepository;
import com.bakery.order.dto.OrderDetailResponse;
import com.bakery.order.dto.OrderItemResponse;
import com.bakery.order.dto.PaymentResponse;
import com.bakery.order.entity.Order;
import com.bakery.order.entity.OrderDetail;
import com.bakery.order.entity.Payment;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckoutOrderMapper {

  private final ProductImageRepository productImages;

  public OrderDetailResponse map(Order order, List<OrderDetail> details, Payment payment) {
    // Batch-load ảnh chính cho tất cả sản phẩm trong đơn (1 query duy nhất).
    // displayOrder ASC → phần tử đầu tiên theo productId là ảnh chính.
    var productIds = details.stream().map(d -> d.getProduct().getId()).toList();
    Map<Long, String> primaryImageByProductId = productImages
        .findByProductIdInOrderByDisplayOrderAscIdAsc(productIds)
        .stream()
        .collect(Collectors.toMap(
            img -> img.getProduct().getId(),
            img -> img.getImagePath(),
            (first, second) -> first   // giữ ảnh displayOrder nhỏ nhất (ảnh chính)
        ));

    var result = new OrderDetailResponse();
    result.setId(order.getId());
    result.setOrderCode(order.getOrderCode());
    result.setAccountId(order.getAccount().getId());
    result.setReceiverName(order.getReceiverName());
    result.setReceiverPhone(order.getReceiverPhone());
    result.setReceiverAddress(order.getReceiverAddress());
    result.setReceiverEmail(order.getReceiverEmail());
    result.setDeliveryDate(order.getDeliveryDate());
    result.setDeliveryTime(order.getDeliveryTime());
    result.setNote(order.getNote());
    result.setTotalAmount(order.getTotalAmount());
    result.setStatus(order.getStatus());
    result.setCancellationReason(order.getCancellationReason());
    result.setCancelledAt(order.getCancelledAt());
    result.setCreatedAt(order.getCreatedAt());
    result.setItems(details.stream().map(detail -> {
      var item = new OrderItemResponse();
      item.setProductId(detail.getProduct().getId());
      item.setProductName(detail.getProductName());
      item.setImagePath(primaryImageByProductId.get(detail.getProduct().getId()));
      item.setUnitPrice(detail.getUnitPrice());
      item.setQuantity(detail.getQuantity());
      item.setLineTotal(detail.getLineTotal());
      item.setNote(detail.getNote());
      return item;
    }).toList());
    result.setPayment(mapPayment(payment));
    return result;
  }

  public PaymentResponse mapPayment(Payment payment) {
    var paymentResponse = new PaymentResponse();
    paymentResponse.setMethod(payment.getMethod());
    paymentResponse.setStatus(payment.getStatus());
    paymentResponse.setAmount(payment.getAmount());
    paymentResponse.setBankReference(payment.getBankReference());
    paymentResponse.setPaidAt(payment.getPaidAt());
    paymentResponse.setConfirmedByName(payment.getConfirmedBy() == null
        ? null : payment.getConfirmedBy().getFullName());
    return paymentResponse;
  }
}
