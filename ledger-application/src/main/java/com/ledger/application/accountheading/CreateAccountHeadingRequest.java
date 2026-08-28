package com.ledger.application.accountheading;

import com.ledger.domain.accountheading.AccountNature;

public record CreateAccountHeadingRequest(
        String parentId,
        String code,
        String name,
        AccountNature nature,
        boolean allowNegativeBalance
) {
}