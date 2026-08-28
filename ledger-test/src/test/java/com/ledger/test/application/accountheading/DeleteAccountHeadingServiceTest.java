package com.ledger.test.application.accountheading;

import com.ledger.application.accountheading.DeleteAccountHeadingRequest;
import com.ledger.application.accountheading.DeleteAccountHeadingResponse;
import com.ledger.application.accountheading.DeleteAccountHeadingService;
import com.ledger.domain.accountheading.AccountHeading;
import com.ledger.domain.accountheading.AccountHeadingId;
import com.ledger.domain.accountheading.AccountHeadingRepository;
import com.ledger.domain.accountheading.AccountNature;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DeleteAccountHeadingServiceTest {

    // =========================================================
    // Delete
    // =========================================================

    @Test
    void shouldDeleteAccountHeading() {

        AccountHeading heading =
                createLeafAccountHeading();

        RecordingRepository repository =
                new RecordingRepository(heading);

        DeleteAccountHeadingService service =
                new DeleteAccountHeadingService(repository);

        DeleteAccountHeadingRequest request =
                new DeleteAccountHeadingRequest(
                        "AH-005"
                );

        DeleteAccountHeadingResponse response =
                service.execute(request);

        assertNotNull(response);

        assertTrue(
                repository.deleteCalled
        );

        assertEquals(
                heading,
                repository.deletedHeading
        );
    }

    // =========================================================
    // Null Request
    // =========================================================

    @Test
    void shouldRejectNullRequest() {

        AccountHeadingRepository repository =
                new RecordingRepository(null);

        DeleteAccountHeadingService service =
                new DeleteAccountHeadingService(repository);

        assertThrows(
                NullPointerException.class,
                () -> service.execute(null)
        );
    }

    // =========================================================
    // AccountHeading Not Found
    // =========================================================

    @Test
    void shouldRejectWhenAccountHeadingDoesNotExist() {

        AccountHeadingRepository repository =
                new RecordingRepository(null);

        DeleteAccountHeadingService service =
                new DeleteAccountHeadingService(repository);

        DeleteAccountHeadingRequest request =
                new DeleteAccountHeadingRequest(
                        "AH-001"
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.execute(request)
        );
    }

    // =========================================================
    // Has Children
    // =========================================================

    @Test
    void shouldRejectWhenAccountHeadingHasChildren() {

        AccountHeading heading =
                createLeafAccountHeading();

        RecordingRepository repository =
                new RecordingRepository(heading);

        repository.hasChildren = true;

        DeleteAccountHeadingService service =
                new DeleteAccountHeadingService(repository);

        DeleteAccountHeadingRequest request =
                new DeleteAccountHeadingRequest(
                        "AH-005"
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.execute(request)
        );

        assertFalse(
                repository.deleteCalled
        );
    }

    // =========================================================
    // Has Accounts
    // =========================================================

    @Test
    void shouldRejectWhenAccountHeadingHasAccounts() {

        AccountHeading heading =
                createLeafAccountHeading();

        RecordingRepository repository =
                new RecordingRepository(heading);

        repository.hasAccounts = true;

        DeleteAccountHeadingService service =
                new DeleteAccountHeadingService(repository);

        DeleteAccountHeadingRequest request =
                new DeleteAccountHeadingRequest(
                        "AH-005"
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.execute(request)
        );

        assertFalse(
                repository.deleteCalled
        );
    }

    // =========================================================
    // Non Leaf
    // =========================================================

    @Test
    void shouldRejectWhenAccountHeadingIsNotLeaf() {

        AccountHeading heading =
                AccountHeading.createRoot(
                        new AccountHeadingId("AH-001"),
                        "100000000",
                        "Root"
                );

        RecordingRepository repository =
                new RecordingRepository(heading);

        DeleteAccountHeadingService service =
                new DeleteAccountHeadingService(repository);

        DeleteAccountHeadingRequest request =
                new DeleteAccountHeadingRequest(
                        "AH-001"
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.execute(request)
        );

        assertFalse(
                repository.deleteCalled
        );
    }

    // =========================================================
    // Test Data
    // =========================================================

    private static AccountHeading createLeafAccountHeading() {

        AccountHeading root =
                AccountHeading.createRoot(
                        new AccountHeadingId("AH-ROOT"),
                        "100000000",
                        "Root"
                );

        AccountHeading level1 =
                root.createFirstLevel(
                        new AccountHeadingId("AH-001"),
                        "101000000",
                        "Level 1",
                        AccountNature.DEBIT,
                        false
                );

        AccountHeading level2 =
                level1.createChild(
                        new AccountHeadingId("AH-002"),
                        "101010000",
                        "Level 2"
                );

        AccountHeading level3 =
                level2.createChild(
                        new AccountHeadingId("AH-003"),
                        "101010100",
                        "Level 3"
                );

        AccountHeading level4 =
                level3.createChild(
                        new AccountHeadingId("AH-004"),
                        "101010110",
                        "Level 4"
                );

        return level4.createChild(
                new AccountHeadingId("AH-005"),
                "101010111",
                "Level 5"
        );
    }

    // =========================================================
    // Repository Stub
    // =========================================================

    private static class RecordingRepository
            implements AccountHeadingRepository {

        private final AccountHeading heading;

        private boolean hasChildren;
        private boolean hasAccounts;

        private boolean deleteCalled;
        private AccountHeading deletedHeading;

        private RecordingRepository(
                AccountHeading heading
        ) {
            this.heading = heading;
        }

        @Override
        public boolean existsByCode(
                String code
        ) {
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

            if (heading == null) {
                return Optional.empty();
            }

            if (heading.getId().equals(id)) {
                return Optional.of(heading);
            }

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
            return List.of();
        }

        @Override
        public boolean hasChildren(
                AccountHeadingId accountHeadingId
        ) {
            return hasChildren;
        }

        @Override
        public boolean hasAccounts(
                AccountHeadingId accountHeadingId
        ) {
            return hasAccounts;
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

            this.deleteCalled = true;
            this.deletedHeading = accountHeading;
        }
    }
}