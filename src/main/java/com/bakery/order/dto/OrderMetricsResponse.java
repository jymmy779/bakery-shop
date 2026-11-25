package com.bakery.order.dto;

public record OrderMetricsResponse(
    long pendingCount,
    long confirmedCount,
    long deliveringCount) {
}
