package com.bakery.notification;

/** Immutable mail snapshot: no managed entities or lazy database reads in the mail thread. */
public record OrderConfirmationMail(String recipient, String subject, String body) {}
