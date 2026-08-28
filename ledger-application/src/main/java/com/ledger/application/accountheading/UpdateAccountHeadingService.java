package com.ledger.application.accountheading;

import com.ledger.domain.accountheading.AccountHeading;
import com.ledger.domain.accountheading.AccountHeadingId;
import com.ledger.domain.accountheading.AccountHeadingRepository;

import java.util.Objects;

public class UpdateAccountHeadingService {

    private final AccountHeadingRepository repository;

    public UpdateAccountHeadingService(
            AccountHeadingRepository repository
    ) {
        this.repository =
                Objects.requireNonNull(
                        repository,
                        "repository must not be null"
                );
    }

    public UpdateAccountHeadingResponse execute(
            UpdateAccountHeadingRequest request
    ) {

        Objects.requireNonNull(
                request,
                "request must not be null"
        );

        AccountHeadingId headingId =
                new AccountHeadingId(request.headingId());

        AccountHeading accountHeading =
                repository.findById(headingId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "AccountHeading not found: "
                                                + request.headingId()
                                )
                        );

        AccountHeading updated =
                accountHeading.rename(request.name());

        repository.save(updated);

        return toResponse(updated);
    }

    private UpdateAccountHeadingResponse toResponse(
            AccountHeading accountHeading
    ) {

        return new UpdateAccountHeadingResponse(
                accountHeading.getId().value(),
                accountHeading.getParentId() != null
                        ? accountHeading.getParentId().value()
                        : null,
                accountHeading.getCode(),
                accountHeading.getName(),
                accountHeading.getLevel(),
                accountHeading.isLeaf()
        );
    }
}