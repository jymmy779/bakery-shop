package com.bakery.order.service.impl;

import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import com.bakery.account.service.AccountService;
import com.bakery.cart.model.Cart;
import com.bakery.catalog.service.ProductStockService;
import com.bakery.catalog.service.ProductStockService.StockRequest;
import com.bakery.common.exception.ConflictException;
import com.bakery.common.exception.InvalidOperationException;
import com.bakery.order.dto.CheckoutRequest;
import com.bakery.order.dto.OrderDetailResponse;
import com.bakery.order.entity.Order;
import com.bakery.order.entity.OrderDetail;
import com.bakery.order.entity.OrderStatus;
import com.bakery.order.entity.OrderStatusHistory;
import com.bakery.order.entity.Payment;
import com.bakery.order.entity.PaymentMethod;
import com.bakery.order.entity.PaymentStatus;
import com.bakery.order.repository.CheckoutReferenceRepository;
import com.bakery.order.repository.OrderDetailRepository;
import com.bakery.order.repository.OrderRepository;
import com.bakery.order.repository.OrderStatusHistoryRepository;
import com.bakery.order.repository.PaymentRepository;
import com.bakery.order.service.CheckoutService;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

  private static final ZoneId SHOP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final BigDecimal MAX_TOTAL = new BigDecimal("9999999999.99");
  private final AccountService accounts;
  private final ProductStockService stock;
  private final OrderRepository orders;
  private final OrderDetailRepository details;
  private final PaymentRepository payments;
  private final OrderStatusHistoryRepository history;
  private final CheckoutReferenceRepository references;
  private final CheckoutOrderMapper mapper;
  private final CheckoutResultServiceImpl results;
  private final Validator validator;

  @Override
  @Transactional
  public OrderDetailResponse checkout(Long accountId, Cart cart, CheckoutRequest request) {
    if (request == null || !validator.validate(request).isEmpty()) {
      throw new InvalidOperationException("Thông tin giao bánh chưa hợp lệ. Vui lòng kiểm tra lại.");
    }
    var account = accounts.getById(accountId);
    if (account.getRole() != Role.ROLE_CUSTOMER || account.getStatus() != AccountStatus.ACTIVE) {
      throw new InvalidOperationException("Tài khoản không được phép đặt hàng.");
    }
    var submitted = results.findSubmitted(accountId, request.getCheckoutToken());
    if (submitted.isPresent()) return submitted.get();
    if (cart == null || cart.getItems().isEmpty()) throw new ConflictException("Giỏ bánh đang trống.");
    if (request.getProductNotes() != null
        && !cart.getItems().keySet().containsAll(request.getProductNotes().keySet())) {
      throw new InvalidOperationException("Lời nhắn chứa bánh không thuộc giỏ hàng hiện tại.");
    }
    var requests = cart.getItems().values().stream()
        .map(item -> new StockRequest(item.getProductId(), item.getQuantity(), item.getUnitPrice())).toList();
    var products = stock.deductForCheckout(requests);
    var earliestDate = LocalDate.now(SHOP_ZONE).plusDays(products.stream()
        .mapToInt(product -> Math.max(0, product.preparationDays())).max().orElse(0));
    if (request.getDeliveryDate().isBefore(earliestDate)) {
      throw new InvalidOperationException("Ngày nhận bánh sớm nhất là "
          + earliestDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".");
    }
    var total = products.stream().map(product -> product.unitPrice()
        .multiply(BigDecimal.valueOf(product.quantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (total.compareTo(MAX_TOTAL) > 0) throw new InvalidOperationException("Tổng tiền đơn hàng vượt giới hạn.");

    var order = new Order();
    order.setOrderCode(CheckoutQuoteStore.orderCode(request.getCheckoutToken()));
    order.setAccount(references.account(accountId));
    order.setReceiverName(request.getReceiverName().trim());
    order.setReceiverPhone(request.getReceiverPhone().trim());
    order.setReceiverAddress(request.getReceiverAddress().trim());
    order.setReceiverEmail(optional(request.getReceiverEmail()));
    order.setDeliveryDate(request.getDeliveryDate());
    order.setDeliveryTime(request.getDeliveryTime());
    order.setNote(optional(request.getNote()));
    order.setTotalAmount(total);
    order.setStatus(OrderStatus.PENDING);
    orders.saveAndFlush(order);

    var orderDetails = new ArrayList<OrderDetail>();
    for (var product : products) {
      var detail = new OrderDetail();
      detail.setOrder(order);
      detail.setProduct(references.product(product.productId()));
      detail.setProductName(product.productName());
      detail.setUnitPrice(product.unitPrice());
      detail.setQuantity(product.quantity());
      detail.setLineTotal(product.unitPrice().multiply(BigDecimal.valueOf(product.quantity())));
      detail.setNote(request.getProductNotes() == null ? null
          : optional(request.getProductNotes().get(product.productId())));
      orderDetails.add(detail);
    }
    details.saveAll(orderDetails);
    var payment = new Payment();
    payment.setOrder(order);
    payment.setMethod(PaymentMethod.BANK_TRANSFER);
    payment.setStatus(PaymentStatus.PENDING);
    payment.setAmount(total);
    payments.saveAndFlush(payment);

    var initial = new OrderStatusHistory();
    initial.setOrder(order);
    initial.setToStatus(OrderStatus.PENDING);
    initial.setChangedBy(references.account(accountId));
    initial.setChangedAt(LocalDateTime.now(SHOP_ZONE));
    initial.setNote("Khách hàng đặt bánh");
    history.saveAndFlush(initial);
    return mapper.map(order, orderDetails, payment);
  }

  private String optional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
