package com.ledger.presentation.accountheading.create;

public record CreateAccountHeadingOutput(
        String id,
        String parentId,
        String code,
        String name,
        int level,
        boolean leaf
) {
}