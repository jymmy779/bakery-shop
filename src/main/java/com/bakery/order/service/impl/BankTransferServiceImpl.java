package com.bakery.order.service.impl;

import com.bakery.order.dto.BankTransferResponse;
import com.bakery.order.dto.OrderDetailResponse;
import com.bakery.order.entity.OrderStatus;
import com.bakery.order.entity.PaymentMethod;
import com.bakery.order.entity.PaymentStatus;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BankTransferServiceImpl {

  private final BankTransferProperties bank;

  /** Called only with the server DTO after the order service has checked customer ownership. */
  public BankTransferResponse prepare(OrderDetailResponse order) {
    var transfer = new BankTransferResponse();
    if (order.getStatus() == OrderStatus.CANCELLED) {
      return unavailable(transfer, "Đơn hàng đã được hủy. Bạn không cần chuyển khoản cho đơn này.");
    }
    var payment = order.getPayment();
    if (payment != null && payment.getStatus() == PaymentStatus.PAID) {
      return unavailable(transfer, "Thanh toán thành công! LUNE đã ghi nhận thanh toán. Cảm ơn bạn!");
    }
    if (order.getStatus() != OrderStatus.PENDING || payment == null
        || payment.getStatus() != PaymentStatus.PENDING || payment.getMethod() != PaymentMethod.BANK_TRANSFER) {
      return unavailable(transfer, "Đơn hàng hiện không còn chờ chuyển khoản. Bạn có thể xem trạng thái mới nhất trong đơn hàng.");
    }
    if (!configured()) {
      return unavailable(transfer, "Thông tin chuyển khoản của LUNE đang được cập nhật. Bạn có thể quay lại chi tiết đơn hàng để xem khi sẵn sàng.");
    }
    // Do not round fractional VND or turn inconsistent stored totals into a payable QR.
    if (payment.getAmount() == null || order.getTotalAmount() == null
        || payment.getAmount().signum() <= 0 || payment.getAmount().compareTo(order.getTotalAmount()) != 0
        || order.getOrderCode() == null || !order.getOrderCode().matches("[A-Za-z0-9]{1,30}")) {
      return unavailable(transfer, "Thông tin thanh toán cần được kiểm tra. Vui lòng liên hệ LUNE và cung cấp mã đơn hàng.");
    }
    String amount;
    try {
      amount = payment.getAmount().toBigIntegerExact().toString();
    } catch (ArithmeticException fractionalAmount) {
      return unavailable(transfer, "Thông tin thanh toán cần được kiểm tra. Vui lòng liên hệ LUNE và cung cấp mã đơn hàng.");
    }
    if (amount.length() > 13) {
      return unavailable(transfer, "Số tiền vượt giới hạn chuyển khoản bằng QR. Vui lòng liên hệ LUNE.");
    }
    transfer.setAvailable(true);
    transfer.setBankName(bank.getBankName());
    transfer.setAccountNumber(bank.getAccountNumber());
    transfer.setAccountName(bank.getAccountName());
    transfer.setAmount(amount);
    transfer.setContent(order.getOrderCode());
    // Fixed provider and validated path segments; encode every query value independently.
    transfer.setQrImageUrl("https://img.vietqr.io/image/" + bank.getBankId() + "-"
        + bank.getAccountNumber() + "-" + bank.getQrTemplate() + ".svg?amount=" + amount
        + "&addInfo=" + encoded(transfer.getContent()) + "&accountName=" + encoded(bank.getAccountName()));
    return transfer;
  }

  private boolean configured() {
    return bank.getBankId().matches("[A-Za-z0-9]{2,30}")
        && !bank.getBankName().isBlank() && bank.getBankName().length() <= 100
        && bank.getAccountNumber().matches("[A-Za-z0-9]{1,19}")
        && !bank.getAccountName().isBlank() && bank.getAccountName().length() <= 100
        && bank.getQrTemplate().matches("[A-Za-z0-9_-]{1,64}");
  }

  private BankTransferResponse unavailable(BankTransferResponse transfer, String notice) {
    transfer.setNotice(notice);
    return transfer;
  }

  private String encoded(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
