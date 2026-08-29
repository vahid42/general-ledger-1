package com.ledger.domain.account;

import com.ledger.domain.common.aggregate.AggregateRoot;
import com.ledger.application.accountheading;

public final class Account extends AggregateRoot<AccountId> {

    public Account(AccountId id) {
        super(id);
    }
}