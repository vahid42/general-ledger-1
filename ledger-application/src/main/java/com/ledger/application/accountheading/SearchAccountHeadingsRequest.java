package com.ledger.application.accountheading;

import com.ledger.domain.accountheading.AccountNature;

public record SearchAccountHeadingsRequest(
        String code,
        String name,
        String parentId,
        Integer level,
        Boolean leaf,
        AccountNature nature
) {
}