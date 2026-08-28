package com.ledger.test.application.accountheading;

import com.ledger.application.accountheading.UpdateAccountHeadingRequest;
import com.ledger.application.accountheading.UpdateAccountHeadingResponse;
import com.ledger.application.accountheading.UpdateAccountHeadingService;
import com.ledger.domain.accountheading.AccountHeading;
import com.ledger.domain.accountheading.AccountHeadingId;
import com.ledger.domain.accountheading.AccountHeadingRepository;
import com.ledger.domain.accountheading.AccountNature;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UpdateAccountHeadingServiceTest {

    // =========================================================
    // Update
    // =========================================================

    @Test
    void shouldUpdateAccountHeadingName() {

        AccountHeadingId id =
                new AccountHeadingId("AH-001");

        AccountHeading heading =
                AccountHeading.createRoot(
                        id,
                        "100000000",
                        "Old Name"
                );

        RecordingRepository repository =
                new RecordingRepository(heading);

        UpdateAccountHeadingService service =
                new UpdateAccountHeadingService(repository);

        UpdateAccountHeadingRequest request =
                new UpdateAccountHeadingRequest(
                        "AH-001",
                        "New Name"
                );

        UpdateAccountHeadingResponse response =
                service.execute(request);

        assertNotNull(response);

        assertEquals(
                "AH-001",
                response.id()
        );

        assertEquals(
                "100000000",
                response.code()
        );

        assertEquals(
                "New Name",
                response.name()
        );

        assertEquals(
                "New Name",
                repository.savedHeading.getName()
        );
    }

    @Test
    void shouldRejectNullRequest() {

        AccountHeadingRepository repository =
                new RecordingRepository(null);

        UpdateAccountHeadingService service =
                new UpdateAccountHeadingService(repository);

        assertThrows(
                NullPointerException.class,
                () -> service.execute(null)
        );
    }

    @Test
    void shouldRejectWhenAccountHeadingDoesNotExist() {

        AccountHeadingRepository repository =
                new RecordingRepository(null);

        UpdateAccountHeadingService service =
                new UpdateAccountHeadingService(repository);

        UpdateAccountHeadingRequest request =
                new UpdateAccountHeadingRequest(
                        "AH-001",
                        "New Name"
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.execute(request)
        );
    }

    @Test
    void shouldNotChangeCode() {

        AccountHeading heading =
                AccountHeading.createRoot(
                        new AccountHeadingId("AH-001"),
                        "100000000",
                        "Old Name"
                );

        RecordingRepository repository =
                new RecordingRepository(heading);

        UpdateAccountHeadingService service =
                new UpdateAccountHeadingService(repository);

        UpdateAccountHeadingRequest request =
                new UpdateAccountHeadingRequest(
                        "AH-001",
                        "New Name"
                );

        UpdateAccountHeadingResponse response =
                service.execute(request);

        assertEquals(
                "100000000",
                response.code()
        );
    }

    @Test
    void shouldRejectBlankName() {

        AccountHeading heading =
                AccountHeading.createRoot(
                        new AccountHeadingId("AH-001"),
                        "100000000",
                        "Old Name"
                );

        AccountHeadingRepository repository =
                new RecordingRepository(heading);

        UpdateAccountHeadingService service =
                new UpdateAccountHeadingService(repository);

        UpdateAccountHeadingRequest request =
                new UpdateAccountHeadingRequest(
                        "AH-001",
                        " "
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.execute(request)
        );
    }

    // =========================================================
    // Repository Stub
    // =========================================================

    private static class RecordingRepository
            implements AccountHeadingRepository {

        private final AccountHeading heading;

        private AccountHeading savedHeading;

        private RecordingRepository(
                AccountHeading heading
        ) {
            this.heading = heading;
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
        }
    }
}