package com.ledger.test.application.accountheading;

import com.ledger.application.accountheading.CreateAccountHeadingRequest;
import com.ledger.application.accountheading.CreateAccountHeadingResponse;
import com.ledger.application.accountheading.CreateAccountHeadingService;
import com.ledger.domain.accountheading.AccountHeading;
import com.ledger.domain.accountheading.AccountHeadingId;
import com.ledger.domain.accountheading.AccountHeadingRepository;
import com.ledger.domain.accountheading.AccountNature;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CreateAccountHeadingServiceTest {

    // =========================================================
    // Create Root
    // =========================================================

    @Test
    void shouldCreateRootAccountHeading() {

        RecordingRepository repository =
                new RecordingRepository();

        CreateAccountHeadingService service =
                new CreateAccountHeadingService(repository);

        CreateAccountHeadingRequest request =
                new CreateAccountHeadingRequest(
                        null,
                        "100000000",
                        "Root",
                        null,
                        false
                );

        CreateAccountHeadingResponse response =
                service.create(request);

        assertNotNull(response);

        assertNotNull(response.id());

        assertNull(response.parentId());

        assertEquals(
                "100000000",
                response.code()
        );

        assertEquals(
                "Root",
                response.name()
        );

        assertEquals(
                0,
                response.level()
        );

        assertFalse(
                response.leaf()
        );

        assertNotNull(
                repository.savedHeading
        );
    }

    // =========================================================
    // Create First Level
    // =========================================================

    @Test
    void shouldCreateFirstLevelAccountHeading() {

        AccountHeadingId parentId =
                new AccountHeadingId("AH-ROOT");

        AccountHeading parent =
                AccountHeading.createRoot(
                        parentId,
                        "100000000",
                        "Root"
                );

        RecordingRepository repository =
                new RecordingRepository(parent);

        CreateAccountHeadingService service =
                new CreateAccountHeadingService(repository);

        CreateAccountHeadingRequest request =
                new CreateAccountHeadingRequest(
                        "AH-ROOT",
                        "101000000",
                        "Cash",
                        AccountNature.DEBIT,
                        true
                );

        CreateAccountHeadingResponse response =
                service.create(request);

        assertNotNull(response);

        assertNotNull(response.id());

        assertEquals(
                "AH-ROOT",
                response.parentId()
        );

        assertEquals(
                "101000000",
                response.code()
        );

        assertEquals(
                "Cash",
                response.name()
        );

        assertEquals(
                1,
                response.level()
        );

        assertFalse(
                response.leaf()
        );

        assertNotNull(
                repository.savedHeading
        );
    }

    // =========================================================
    // Create Child
    // =========================================================

    @Test
    void shouldCreateChildAccountHeading() {

        AccountHeading root =
                AccountHeading.createRoot(
                        new AccountHeadingId("AH-ROOT"),
                        "100000000",
                        "Root"
                );

        AccountHeading parent =
                root.createFirstLevel(
                        new AccountHeadingId("AH-001"),
                        "101000000",
                        "Assets",
                        AccountNature.DEBIT,
                        true
                );

        RecordingRepository repository =
                new RecordingRepository(parent);

        CreateAccountHeadingService service =
                new CreateAccountHeadingService(repository);

        CreateAccountHeadingRequest request =
                new CreateAccountHeadingRequest(
                        "AH-001",
                        "101010000",
                        "Cash",
                        null,
                        false
                );

        CreateAccountHeadingResponse response =
                service.create(request);

        assertNotNull(response);

        assertNotNull(response.id());

        assertEquals(
                "AH-001",
                response.parentId()
        );

        assertEquals(
                "101010000",
                response.code()
        );

        assertEquals(
                "Cash",
                response.name()
        );

        assertEquals(
                2,
                response.level()
        );

        assertFalse(
                response.leaf()
        );

        assertNotNull(
                repository.savedHeading
        );
    }

    // =========================================================
    // Duplicate Code
    // =========================================================

    @Test
    void shouldRejectDuplicateCode() {

        AccountHeading existingHeading =
                AccountHeading.createRoot(
                        new AccountHeadingId("AH-001"),
                        "100000000",
                        "Existing"
                );

        RecordingRepository repository =
                new RecordingRepository(existingHeading);

        CreateAccountHeadingService service =
                new CreateAccountHeadingService(repository);

        CreateAccountHeadingRequest request =
                new CreateAccountHeadingRequest(
                        null,
                        "100000000",
                        "Another Root",
                        null,
                        false
                );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> service.create(request)
                );

        assertEquals(
                "Account heading code already exists: 100000000",
                exception.getMessage()
        );

        assertNull(
                repository.savedHeading
        );
    }

    // =========================================================
    // Parent Not Found
    // =========================================================

    @Test
    void shouldRejectWhenParentDoesNotExist() {

        RecordingRepository repository =
                new RecordingRepository();

        CreateAccountHeadingService service =
                new CreateAccountHeadingService(repository);

        CreateAccountHeadingRequest request =
                new CreateAccountHeadingRequest(
                        "AH-UNKNOWN",
                        "101000000",
                        "Cash",
                        AccountNature.DEBIT,
                        true
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.create(request)
                );

        assertEquals(
                "Parent AccountHeading not found: AH-UNKNOWN",
                exception.getMessage()
        );

        assertNull(
                repository.savedHeading
        );
    }

    // =========================================================
    // Null Request
    // =========================================================

    @Test
    void shouldRejectNullRequest() {

        RecordingRepository repository =
                new RecordingRepository();

        CreateAccountHeadingService service =
                new CreateAccountHeadingService(repository);

        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> service.create(null)
                );

        assertEquals(
                "request must not be null",
                exception.getMessage()
        );
    }

    // =========================================================
    // Repository Stub
    // =========================================================

    private static class RecordingRepository
            implements AccountHeadingRepository {

        private final AccountHeading existingHeading;

        private AccountHeading savedHeading;

        private RecordingRepository() {
            this.existingHeading = null;
        }

        private RecordingRepository(
                AccountHeading existingHeading
        ) {
            this.existingHeading = existingHeading;
        }

        @Override
        public boolean existsByCode(
                String code
        ) {

            if (existingHeading == null) {
                return false;
            }

            return existingHeading
                    .getCode()
                    .equals(code);
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

            if (existingHeading == null) {
                return Optional.empty();
            }

            if (existingHeading.getId().equals(id)) {
                return Optional.of(existingHeading);
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
            this.savedHeading = accountHeading;
        }

        @Override
        public void delete(
                AccountHeading accountHeading
        ) {
            // No-op for test repository
        }
    }
}