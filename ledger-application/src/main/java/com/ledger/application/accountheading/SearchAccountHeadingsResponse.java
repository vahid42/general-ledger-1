package com.ledger.application.accountheading;

import java.util.List;

public record SearchAccountHeadingsResponse(
        List<AccountHeadingItem> items
) {

    public record AccountHeadingItem(
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
}