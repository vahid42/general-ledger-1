package com.ledger.domain.account;

import java.util.Objects;
import java.util.UUID;

import com.ledger.domain.common.valueobject.ValueObject;

public record AccountId(String value)  implements ValueObject {

    public AccountId {
        Objects.requireNonNull(value, "AccountId value must not be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException("AccountId value must not be blank");
        }
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID().toString());
    }
}