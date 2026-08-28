package com.ledger.test.application.accountheading;

import com.ledger.application.accountheading.SearchAccountHeadingsRequest;
import com.ledger.application.accountheading.SearchAccountHeadingsResponse;
import com.ledger.application.accountheading.SearchAccountHeadingsService;
import com.ledger.domain.accountheading.AccountHeading;
import com.ledger.domain.accountheading.AccountHeadingId;
import com.ledger.domain.accountheading.AccountHeadingRepository;
import com.ledger.domain.accountheading.AccountNature;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SearchAccountHeadingsServiceTest {

    // =========================================================
    // Search
    // =========================================================

    @Test
    void shouldReturnMatchingAccountHeadings() {

        AccountHeading heading =
                AccountHeading.createRoot(
                        new AccountHeadingId("AH-001"),
                        "100000000",
                        "Root"
                );

        AccountHeadingRepository repository =
                new StubAccountHeadingRepository(
                        List.of(heading)
                );

        SearchAccountHeadingsService service =
                new SearchAccountHeadingsService(repository);

        SearchAccountHeadingsRequest request =
                new SearchAccountHeadingsRequest(
                        null,
                        "Root",
                        null,
                        0,
                        false,
                        null
                );

        SearchAccountHeadingsResponse response =
                service.execute(request);

        assertEquals(
                1,
                response.items().size()
        );

        SearchAccountHeadingsResponse.AccountHeadingItem item =
                response.items().get(0);

        assertEquals(
                "AH-001",
                item.id()
        );

        assertNull(
                item.parentId()
        );

        assertEquals(
                "100000000",
                item.code()
        );

        assertEquals(
                "Root",
                item.name()
        );

        assertNull(
                item.nature()
        );

        assertFalse(
                item.allowNegativeBalance()
        );

        assertEquals(
                0,
                item.level()
        );

        assertFalse(
                item.leaf()
        );
    }

    @Test
    void shouldReturnEmptyResultWhenNoHeadingMatches() {

        AccountHeadingRepository repository =
                new StubAccountHeadingRepository(
                        List.of()
                );

        SearchAccountHeadingsService service =
                new SearchAccountHeadingsService(repository);

        SearchAccountHeadingsRequest request =
                new SearchAccountHeadingsRequest(
                        "101000000",
                        null,
                        null,
                        null,
                        null,
                        null
                );

        SearchAccountHeadingsResponse response =
                service.execute(request);

        assertNotNull(response);

        assertTrue(
                response.items().isEmpty()
        );
    }

    @Test
    void shouldRejectNullRequest() {

        AccountHeadingRepository repository =
                new StubAccountHeadingRepository(
                        List.of()
                );

        SearchAccountHeadingsService service =
                new SearchAccountHeadingsService(repository);

        assertThrows(
                NullPointerException.class,
                () -> service.execute(null)
        );
    }

    // =========================================================
    // Repository Stub
    // =========================================================

    private static class StubAccountHeadingRepository
            implements AccountHeadingRepository {

        private final List<AccountHeading> headings;

        private StubAccountHeadingRepository(
                List<AccountHeading> headings
        ) {
            this.headings = headings;
        }

        @Override
        public boolean existsByCode(String code) {
            return false;
        }

        @Override
        public boolean existsByCodeAndIdNot(
                String code,
                AccountHeadingId id
        ) {
            return false;
        }

        @Override
        public Optional<AccountHeading> findById(
                AccountHeadingId id
        ) {
            return Optional.empty();
        }

        @Override
        public Optional<AccountHeading> findByCode(
                String code
        ) {
            return Optional.empty();
        }

        @Override
        public List<AccountHeading> search(
                String code,
                String name,
                AccountHeadingId parentId,
                Integer level,
                Boolean leaf,
                AccountNature nature
        ) {
            return headings;
        }

        @Override
        public boolean hasChildren(
                AccountHeadingId accountHeadingId
        ) {
            return false;
        }

        @Override
        public boolean hasAccounts(
                AccountHeadingId accountHeadingId
        ) {
            return false;
        }

        @Override
        public void save(
                AccountHeading accountHeading
        ) {
        }

        @Override
        public void delete(
                AccountHeading accountHeading
        ) {
        }
    }
}