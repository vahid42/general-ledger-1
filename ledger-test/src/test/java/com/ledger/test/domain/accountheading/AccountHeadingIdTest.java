package com.ledger.test.domain.accountheading;

import org.junit.jupiter.api.Test;

import com.ledger.domain.accountheading.AccountHeadingId;

import static org.junit.jupiter.api.Assertions.*;

class AccountHeadingIdTest {

    @Test
    void shouldCreateAccountHeadingId() {
        AccountHeadingId id = new AccountHeadingId("AH-001");

        assertEquals("AH-001", id.value());
    }

    @Test
    void shouldRejectNullValue() {
        assertThrows(
                NullPointerException.class,
                () -> new AccountHeadingId(null)
        );
    }

    @Test
    void shouldRejectBlankValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AccountHeadingId(" ")
        );
    }

    @Test
    void shouldGenerateAccountHeadingId() {
        AccountHeadingId id = AccountHeadingId.generate();

        assertNotNull(id);
        assertNotNull(id.value());
        assertFalse(id.value().isBlank());
    }

    @Test
    void shouldSupportValueEquality() {
        AccountHeadingId first = new AccountHeadingId("AH-001");
        AccountHeadingId second = new AccountHeadingId("AH-001");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldNotBeEqualToDifferentAccountHeadingId() {
        AccountHeadingId first = new AccountHeadingId("AH-001");
        AccountHeadingId second = new AccountHeadingId("AH-002");

        assertNotEquals(first, second);
    } 

    
}