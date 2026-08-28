package com.ledger.application.accountheading;

public record UpdateAccountHeadingRequest(
        String headingId,
        String name
) {
}