package com.ledger.domain.accountheading;

import java.util.List;
import java.util.Optional;

public interface AccountHeadingRepository {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, AccountHeadingId id);

    Optional<AccountHeading> findById(AccountHeadingId id);

    Optional<AccountHeading> findByCode(String code);

    void save(AccountHeading accountHeading);

    boolean hasChildren(AccountHeadingId accountHeadingId);

    void delete(AccountHeading accountHeading);

    boolean hasAccounts(AccountHeadingId accountHeadingId);

    List<AccountHeading> search(
            String code,
            String name,
            AccountHeadingId parentId,
            Integer level,
            Boolean leaf,
            AccountNature nature
    );

}