package com.ledger.application.accountheading;

public record UpdateAccountHeadingResponse(
        String id,
        String parentId,
        String code,
        String name,
        int level,
        boolean leaf
) {
}