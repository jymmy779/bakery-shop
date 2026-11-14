package com.bakery.order.service.impl;

import com.bakery.order.dto.OrderDetailResponse;
import com.bakery.order.dto.OrderItemResponse;
import com.bakery.order.dto.PaymentResponse;
import com.bakery.order.entity.Order;
import com.bakery.order.entity.OrderDetail;
import com.bakery.order.entity.Payment;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CheckoutOrderMapper {

  public OrderDetailResponse map(Order order, List<OrderDetail> details, Payment payment) {
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
    result.setCreatedAt(order.getCreatedAt());
    result.setItems(details.stream().map(detail -> {
      var item = new OrderItemResponse();
      item.setProductId(detail.getProduct().getId());
      item.setProductName(detail.getProductName());
      item.setUnitPrice(detail.getUnitPrice());
      item.setQuantity(detail.getQuantity());
      item.setLineTotal(detail.getLineTotal());
      item.setNote(detail.getNote());
      return item;
    }).toList());
    var paymentResponse = new PaymentResponse();
    paymentResponse.setMethod(payment.getMethod());
    paymentResponse.setStatus(payment.getStatus());
    paymentResponse.setAmount(payment.getAmount());
    paymentResponse.setBankReference(payment.getBankReference());
    paymentResponse.setPaidAt(payment.getPaidAt());
    result.setPayment(paymentResponse);
    return result;
  }
}
