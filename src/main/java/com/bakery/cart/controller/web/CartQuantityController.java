package com.bakery.cart.controller.web;

import com.bakery.cart.dto.CartResponse;
import com.bakery.cart.dto.AddToCartRequest;
import com.bakery.cart.dto.UpdateCartItemRequest;
import com.bakery.cart.service.impl.SessionCartServiceImpl;
import jakarta.servlet.http.HttpSession;
import com.bakery.common.exception.ConflictException;
import com.bakery.common.exception.InvalidOperationException;
import com.bakery.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/cart/items")
@RequiredArgsConstructor
public class CartQuantityController {

  private final SessionCartServiceImpl service;

  @GetMapping
  public CartResponse get(HttpSession session) {
    return service.getCart(session);
  }

  @PostMapping
  public CartResponse add(HttpSession session, @Valid @RequestBody AddToCartRequest request) {
    return service.addItem(session, request);
  }

  @PatchMapping("/{productId}")
  public CartResponse update(HttpSession session,
      @PathVariable Long productId, @Valid @RequestBody UpdateCartItemRequest request) {
    return service.updateItem(session, productId, request);
  }

  @DeleteMapping("/{productId}")
  public CartResponse remove(HttpSession session, @PathVariable Long productId) {
    return service.removeItem(session, productId);
  }

  @ExceptionHandler({InvalidOperationException.class, MethodArgumentNotValidException.class,
      HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
  public ResponseEntity<Map<String, String>> badRequest(Exception exception) {
    String message = exception instanceof InvalidOperationException
        ? exception.getMessage() : "Dữ liệu không hợp lệ; quantity phải là số nguyên từ 1 trở lên";
    return ResponseEntity.badRequest().body(Map.of("message", message));
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, String>> notFound(ResourceNotFoundException exception) {
    return ResponseEntity.status(404).body(Map.of("message", exception.getMessage()));
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<Map<String, String>> conflict(ConflictException exception) {
    return ResponseEntity.status(409).body(Map.of("message", exception.getMessage()));
  }
}
