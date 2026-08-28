package com.ledger.domain.accountheading;

import java.util.Objects;
import java.util.UUID;

import com.ledger.domain.common.valueobject.ValueObject;

public record AccountHeadingId(String value)  implements ValueObject {

    public AccountHeadingId {
        Objects.requireNonNull(value, "AccountHeadingId value must not be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException("AccountHeadingId value must not be blank");
        }
    }

    public static AccountHeadingId generate() {
        return new AccountHeadingId(UUID.randomUUID().toString());
    }
}