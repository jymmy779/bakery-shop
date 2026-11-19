package com.bakery.order.controller.api;

import com.bakery.order.dto.SepayWebhookRequest;
import com.bakery.order.service.impl.SepayWebhookServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class SepayWebhookController {

  private final SepayWebhookServiceImpl sepayWebhookService;

  @Value("${app.sepay.api-key:}")
  private String sepayApiKey;

  @PostMapping("/sepay")
  public ResponseEntity<?> handleSepayWebhook(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestBody SepayWebhookRequest payload) {

    log.info("Nhận webhook từ SePay: Nội dung = {}, Số tiền = {}",
        payload.getContent(), payload.getTransferAmount());

    // 1. Kiểm tra API Key từ SePay
    if (sepayApiKey != null && !sepayApiKey.isBlank()) {
      String expected = "Apikey " + sepayApiKey;
      if (authHeader == null || !authHeader.equals(expected)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Sai API Key");
      }
    }

    // 2. Bỏ qua nếu không phải tiền vào
    if (!"in".equalsIgnoreCase(payload.getTransferType())) {
      return ResponseEntity.ok(Map.of("success", true, "message", "Bỏ qua tiền ra"));
    }

    // 3. Xử lý cập nhật đơn hàng
    try {
      sepayWebhookService.processWebhook(payload);
      return ResponseEntity.ok(Map.of("success", true));
    } catch (Exception e) {
      log.error("Lỗi xử lý webhook SePay: ", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("success", false, "error", e.getMessage()));
    }
  }
}