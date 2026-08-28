package com.ledger.domain.journal;

import java.util.Objects;
import java.util.UUID;

import com.ledger.domain.common.valueobject.ValueObject;

public record JournalId(String value) implements ValueObject{

    public JournalId {
        Objects.requireNonNull(value, "JournalId value must not be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException("JournalId value must not be blank");
        }
    }

    public static JournalId generate() {
        return new JournalId(UUID.randomUUID().toString());
    }
}