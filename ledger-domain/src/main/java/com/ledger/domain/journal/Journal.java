package com.ledger.domain.journal;

import com.ledger.domain.common.aggregate.AggregateRoot;

public final class Journal extends AggregateRoot<JournalId> {

    public Journal(JournalId id) {
        super(id);
    }
}