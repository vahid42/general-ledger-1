package com.ledger.test.domain.account;

import org.junit.jupiter.api.Test;

import com.ledger.domain.account.AccountId;

import static org.junit.jupiter.api.Assertions.*;

class AccountIdTest {

    @Test
    void shouldCreateAccountId() {
        AccountId id = new AccountId("ACC-001");

        assertEquals("ACC-001", id.value());
    }

    @Test
    void shouldRejectNullValue() {
        assertThrows(
                NullPointerException.class,
                () -> new AccountId(null)
        );
    }

    @Test
    void shouldRejectBlankValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AccountId(" ")
        );
    }

    @Test
    void shouldGenerateAccountId() {
        AccountId id = AccountId.generate();

        assertNotNull(id);
        assertNotNull(id.value());
        assertFalse(id.value().isBlank());
    }

    @Test
    void shouldSupportValueEquality() {
        AccountId first = new AccountId("ACC-001");
        AccountId second = new AccountId("ACC-001");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldNotBeEqualToDifferentAccountId() {
        AccountId first = new AccountId("ACC-001");
        AccountId second = new AccountId("ACC-002");

        assertNotEquals(first, second);
    }
}