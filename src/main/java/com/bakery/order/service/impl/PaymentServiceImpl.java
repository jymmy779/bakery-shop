package com.bakery.order.service.impl;

import com.bakery.common.exception.ConflictException;
import com.bakery.common.exception.InvalidOperationException;
import com.bakery.common.exception.ResourceNotFoundException;
import com.bakery.order.dto.PaymentConfirmationRequest;
import com.bakery.order.dto.PaymentResponse;
import com.bakery.order.entity.OrderStatus;
import com.bakery.order.entity.OrderStatusHistory;
import com.bakery.order.entity.PaymentMethod;
import com.bakery.order.entity.PaymentStatus;
import com.bakery.order.repository.CheckoutReferenceRepository;
import com.bakery.order.repository.OrderRepository;
import com.bakery.order.repository.OrderStatusHistoryRepository;
import com.bakery.order.repository.PaymentRepository;
import com.bakery.order.service.PaymentService;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

  private final PaymentStaffAccess staffAccess;
  private final OrderRepository orders;
  private final PaymentRepository payments;
  private final OrderStatusHistoryRepository history;
  private final CheckoutReferenceRepository references;
  private final CheckoutOrderMapper mapper;
  private final Validator validator;

  @Override
  @Transactional
  public PaymentResponse confirmBankTransfer(Long orderId, Long confirmedByAccountId,
      PaymentConfirmationRequest request) {
    staffAccess.requireActiveStaff(confirmedByAccountId);
    if (request == null || !validator.validate(request).isEmpty()) {
      throw new InvalidOperationException("Mã tham chiếu ngân hàng tối đa 100 ký tự.");
    }
    // Every payment confirmation takes the order lock first, including concurrent requests.
    var order = orders.findForPaymentConfirmation(orderId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng."));
    if (order.getStatus() != OrderStatus.PENDING) {
      throw new ConflictException("Chỉ xác nhận thanh toán cho đơn đang chờ xử lý. Vui lòng kiểm tra trạng thái mới nhất.");
    }
    var payment = payments.findByOrder_Id(orderId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thanh toán của đơn hàng."));
    if (payment.getMethod() != PaymentMethod.BANK_TRANSFER || payment.getStatus() != PaymentStatus.PENDING) {
      throw new ConflictException("Thanh toán không còn ở trạng thái chờ chuyển khoản.");
    }
    if (payment.getAmount() == null || payment.getAmount().signum() <= 0
        || order.getTotalAmount() == null || payment.getAmount().compareTo(order.getTotalAmount()) != 0) {
      throw new ConflictException("Số tiền thanh toán không khớp tổng đơn. Cần kiểm tra lại trước khi xác nhận.");
    }
    var now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    var actor = references.account(confirmedByAccountId);
    payment.setStatus(PaymentStatus.PAID);
    payment.setPaidAt(now);
    payment.setConfirmedBy(actor);
    payment.setBankReference(request.getBankReference() == null || request.getBankReference().isBlank()
        ? null : request.getBankReference().trim());
    order.setStatus(OrderStatus.CONFIRMED);

    var event = new OrderStatusHistory();
    event.setOrder(order);
    event.setFromStatus(OrderStatus.PENDING);
    event.setToStatus(OrderStatus.CONFIRMED);
    event.setChangedBy(actor);
    event.setChangedAt(now);
    event.setNote("Đã đối soát và xác nhận nhận đủ tiền chuyển khoản.");
    // Flush all three writes together; a failure rolls back payment, order and history.
    history.saveAndFlush(event);
    return mapper.mapPayment(payment);
  }
}
