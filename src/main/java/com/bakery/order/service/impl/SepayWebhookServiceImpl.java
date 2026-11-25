package com.bakery.order.service.impl;

import com.bakery.order.dto.SepayWebhookRequest;
import com.bakery.order.entity.OrderStatus;
import com.bakery.order.entity.OrderStatusHistory;
import com.bakery.order.entity.PaymentStatus;
import com.bakery.order.repository.OrderRepository;
import com.bakery.order.repository.OrderStatusHistoryRepository;
import com.bakery.order.repository.PaymentRepository;
import com.bakery.order.service.SepayWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
// ... các import khác giữ nguyên

@Service
@RequiredArgsConstructor
@Slf4j
public class SepayWebhookServiceImpl implements SepayWebhookService {

  private static final Pattern ORDER_CODE_PATTERN = Pattern.compile("LN[A-Za-z0-9]+");
  private final OrderRepository orderRepository;
  private final PaymentRepository paymentRepository;
  private final OrderStatusHistoryRepository historyRepository;

  @Override
  @Transactional
  public void processWebhook(SepayWebhookRequest request) {
    if (request == null || request.getContent() == null) {
      log.warn("SePay webhook rỗng hoặc không có nội dung chuyển khoản.");
      return;
    }
    // 1. Tìm mã đơn hàng LN... trong nội dung chuyển khoản
    Matcher matcher = ORDER_CODE_PATTERN.matcher(request.getContent().toUpperCase());
    if (!matcher.find()) {
      log.warn("Không tìm thấy mã đơn hàng LN trong nội dung: {}", request.getContent());
      return;
    }
    String orderCode = matcher.group();
    log.info("Tìm thấy mã đơn hàng: {}", orderCode);
    // 2. Tìm đơn hàng trong DB
    var orderOpt = orderRepository.findByOrderCode(orderCode);
    if (orderOpt.isEmpty()) {
      log.warn("Không tìm thấy đơn hàng có mã: {}", orderCode);
      return;
    }
    var order = orderOpt.get();
    // 3. Kiểm tra đơn hàng có đang chờ thanh toán không
    if (order.getStatus() != OrderStatus.PENDING) {
      log.info("Đơn hàng {} hiện có trạng thái {}, không cần xử lý.", orderCode, order.getStatus());
      return;
    }
    var payment = paymentRepository.findByOrder_Id(order.getId()).orElse(null);
    if (payment == null || payment.getStatus() == PaymentStatus.PAID) {
      log.info("Thanh toán của đơn {} đã hoàn tất trước đó.", orderCode);
      return;
    }
    // 4. Kiểm tra số tiền chuyển có đủ không
    if (request.getTransferAmount() != null
      && order.getTotalAmount() != null
      && request.getTransferAmount().compareTo(order.getTotalAmount()) < 0) {
      log.warn("Đơn {}: Số tiền chuyển ({}) ít hơn tổng tiền đơn ({})",
        orderCode, request.getTransferAmount(), order.getTotalAmount());
      return;
    }
    // 5. Cập nhật trạng thái thanh toán và đơn hàng
    var now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    payment.setStatus(PaymentStatus.PAID);
    payment.setPaidAt(now);
    payment.setBankReference(request.getReferenceCode());
    order.setStatus(OrderStatus.CONFIRMED);
    var history = new OrderStatusHistory();
    history.setOrder(order);
    history.setFromStatus(OrderStatus.PENDING);
    history.setToStatus(OrderStatus.CONFIRMED);
    history.setChangedAt(now);
    history.setNote("Tự động xác nhận thanh toán qua SePay. Mã GD: " + request.getReferenceCode());
    paymentRepository.save(payment);
    orderRepository.save(order);
    historyRepository.save(history);
    log.info("Đã cập nhật đơn hàng {} thành công sang CONFIRMED và PAID!", orderCode);

  }
}
