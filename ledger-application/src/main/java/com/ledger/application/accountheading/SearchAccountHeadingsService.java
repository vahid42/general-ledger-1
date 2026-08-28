package com.ledger.application.accountheading;

import com.ledger.domain.accountheading.AccountHeading;
import com.ledger.domain.accountheading.AccountHeadingId;
import com.ledger.domain.accountheading.AccountHeadingRepository;
import com.ledger.domain.accountheading.AccountNature;

import java.util.List;
import java.util.Objects;

public class SearchAccountHeadingsService {

    private final AccountHeadingRepository repository;

    public SearchAccountHeadingsService(
            AccountHeadingRepository repository
    ) {
        this.repository =
                Objects.requireNonNull(
                        repository,
                        "repository must not be null"
                );
    }

    public SearchAccountHeadingsResponse execute(
            SearchAccountHeadingsRequest request
    ) {

        Objects.requireNonNull(
                request,
                "request must not be null"
        );

        AccountHeadingId parentId =
                request.parentId() != null
                        ? new AccountHeadingId(request.parentId())
                        : null;

        AccountNature nature =
                request.nature();
                
        List<AccountHeading> headings =
                repository.search(
                        request.code(),
                        request.name(),
                        parentId,
                        request.level(),
                        request.leaf(),
                        nature
                );

        List<SearchAccountHeadingsResponse.AccountHeadingItem> items =
                headings.stream()
                        .map(this::toItem)
                        .toList();

        return new SearchAccountHeadingsResponse(items);
    }

    private SearchAccountHeadingsResponse.AccountHeadingItem toItem(
            AccountHeading accountHeading
    ) {

        return new SearchAccountHeadingsResponse.AccountHeadingItem(
                accountHeading.getId().value(),

                accountHeading.getParentId() != null
                        ? accountHeading.getParentId().value()
                        : null,

                accountHeading.getCode(),
                accountHeading.getName(),

                accountHeading.getNature() != null
                        ? accountHeading.getNature().name()
                        : null,

                accountHeading.isAllowNegativeBalance(),
                accountHeading.getLevel(),
                accountHeading.isLeaf()
        );
    }
}