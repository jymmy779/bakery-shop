package com.bakery.order.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SepayWebhookRequest {
    private Long id; // ID giao dịch trên SePay
    private String gateway; // Tên ngân hàng
    private String transactionDate; // Ngày giờ giao dịch
    private String accountNumber; // Số tài khoản nhận
    private String content; // Nội dung chuyển khoản (chứa mã đơn hàng)
    private String transferType; // "in" (tiền vào) hoặc "out" (tiền ra)
    private BigDecimal transferAmount;// Số tiền nhận được
    private String referenceCode; // Mã tham chiếu ngân hàng
}