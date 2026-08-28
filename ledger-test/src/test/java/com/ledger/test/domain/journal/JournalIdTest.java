package com.ledger.test.domain.journal;

import org.junit.jupiter.api.Test;

import com.ledger.domain.journal.JournalId;

import static org.junit.jupiter.api.Assertions.*;

class JournalIdTest {

    @Test
    void shouldCreateJournalId() {
        JournalId id = new JournalId("J-001");

        assertEquals("J-001", id.value());
    }

    @Test
    void shouldRejectNullValue() {
        assertThrows(
                NullPointerException.class,
                () -> new JournalId(null)
        );
    }

    @Test
    void shouldRejectBlankValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new JournalId(" ")
        );
    }

    @Test
    void shouldGenerateJournalId() {
        JournalId id = JournalId.generate();

        assertNotNull(id);
        assertNotNull(id.value());
        assertFalse(id.value().isBlank());
    }

    @Test
    void shouldSupportValueEquality() {
        JournalId first = new JournalId("J-001");
        JournalId second = new JournalId("J-001");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldNotBeEqualToDifferentJournalId() {
        JournalId first = new JournalId("J-001");
        JournalId second = new JournalId("J-002");

        assertNotEquals(first, second);
    }
}