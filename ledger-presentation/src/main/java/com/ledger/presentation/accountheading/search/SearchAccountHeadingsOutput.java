package com.ledger.presentation.accountheading.search;

public record SearchAccountHeadingsOutput(
        String id,
        String parentId,
        String code,
        String name,
        String nature,
        boolean allowNegativeBalance,
        int level,
        boolean leaf
) {
}