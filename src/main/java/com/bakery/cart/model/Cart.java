package com.bakery.cart.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Cart implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private Map<Long, CartItem> items = new LinkedHashMap<>();
}
