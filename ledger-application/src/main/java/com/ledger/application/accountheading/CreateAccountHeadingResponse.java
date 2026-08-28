package com.ledger.application.accountheading;

public record CreateAccountHeadingResponse(
        String id,
        String parentId,
        String code,
        String name,
        int level,
        boolean leaf
) {
}