package com.ledger.presentation.accountheading.update;

public record UpdateAccountHeadingOutput(
        String id,
        String parentId,
        String code,
        String name,
        int level,
        boolean leaf
) {
}