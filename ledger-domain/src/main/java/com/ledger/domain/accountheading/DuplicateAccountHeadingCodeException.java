package com.ledger.domain.accountheading;

public class DuplicateAccountHeadingCodeException
        extends RuntimeException {

    public DuplicateAccountHeadingCodeException(String code) {
        super("AccountHeading code already exists: " + code);
    }
}