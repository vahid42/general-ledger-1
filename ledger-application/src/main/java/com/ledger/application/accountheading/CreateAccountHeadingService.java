package com.ledger.application.accountheading;

import com.ledger.domain.accountheading.AccountHeading;
import com.ledger.domain.accountheading.AccountHeadingId;
import com.ledger.domain.accountheading.AccountHeadingRepository;

import java.util.Objects;

public class CreateAccountHeadingService {

    private final AccountHeadingRepository repository;

    public CreateAccountHeadingService(
            AccountHeadingRepository repository
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "repository must not be null"
        );
    }

    public CreateAccountHeadingResponse create(
            CreateAccountHeadingRequest request
    ) {

        Objects.requireNonNull(
                request,
                "request must not be null"
        );

        // ---------------------------------------------------------
        // External Policy
        // ---------------------------------------------------------
        // Code uniqueness cannot be determined from the
        // AccountHeading aggregate state alone.
        // Therefore, the Application Service queries the repository
        // before creating the aggregate.
        // ---------------------------------------------------------

        validateCodeUniqueness(request.code());

        // ---------------------------------------------------------
        // Create Aggregate
        // ---------------------------------------------------------

        AccountHeading accountHeading;

        if (request.parentId() == null) {

            accountHeading = AccountHeading.createRoot(
                    AccountHeadingId.generate(),
                    request.code(),
                    request.name()
            );

        } else {

            AccountHeadingId parentId =
                    new AccountHeadingId(request.parentId());

            AccountHeading parent = repository
                    .findById(parentId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Parent AccountHeading not found: "
                                            + request.parentId()
                            )
                    );

            if (parent.isRoot()) {

                accountHeading = parent.createFirstLevel(
                        AccountHeadingId.generate(),
                        request.code(),
                        request.name(),
                        request.nature(),
                        request.allowNegativeBalance()
                );

            } else {

                accountHeading = parent.createChild(
                        AccountHeadingId.generate(),
                        request.code(),
                        request.name()
                );
            }
        }

        // ---------------------------------------------------------
        // Persist
        // ---------------------------------------------------------

        repository.save(accountHeading);

        // ---------------------------------------------------------
        // Response
        // ---------------------------------------------------------

        return new CreateAccountHeadingResponse(
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

    // ---------------------------------------------------------
    // External Policy
    // ---------------------------------------------------------

    private void validateCodeUniqueness(String code) {

        if (repository.existsByCode(code)) {
            throw new IllegalStateException(
                    "Account heading code already exists: " + code
            );
        }
    }
}