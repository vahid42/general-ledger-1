package com.ledger.application.accountheading;

import com.ledger.domain.accountheading.AccountHeading;
import com.ledger.domain.accountheading.AccountHeadingId;
import com.ledger.domain.accountheading.AccountHeadingRepository;

import java.util.Objects;

public class DeleteAccountHeadingService {

    private final AccountHeadingRepository repository;

    public DeleteAccountHeadingService(
            AccountHeadingRepository repository
    ) {
        this.repository =
                Objects.requireNonNull(
                        repository,
                        "repository must not be null"
                );
    }

    public DeleteAccountHeadingResponse execute(
            DeleteAccountHeadingRequest request
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

        if (!accountHeading.isLeaf()) {
            throw new IllegalStateException(
                    "Only leaf AccountHeading can be deleted"
            );
        }

        if (repository.hasChildren(headingId)) {
            throw new IllegalStateException(
                    "AccountHeading with children cannot be deleted"
            );
        }

        if (repository.hasAccounts(headingId)) {
            throw new IllegalStateException(
                    "AccountHeading with accounts cannot be deleted"
            );
        }

        repository.delete(accountHeading);

        return new DeleteAccountHeadingResponse(
                accountHeading.getId().value()
        );
    }
}