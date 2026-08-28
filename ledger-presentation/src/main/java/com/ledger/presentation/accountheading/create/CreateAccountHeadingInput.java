package com.ledger.presentation.accountheading.create;

import com.ledger.domain.accountheading.AccountNature;

public record CreateAccountHeadingInput(
        String parentId,
        String code,
        String name,
        AccountNature nature,
        boolean allowNegativeBalance
) {
}