package com.bakery.order.service;

import com.bakery.order.dto.SepayWebhookRequest;

public interface SepayWebhookService {

  void processWebhook(SepayWebhookRequest request);

}
