package com.ledger.presentation.accountheading.search;


public record SearchAccountHeadingsInput(
        String code,
        String name,
        String parentId,
        Integer level,
        Boolean leaf,
        String nature
) {
}