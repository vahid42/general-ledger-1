package com.ledger.infrastructure.accountheading;

import com.ledger.domain.accountheading.AccountHeading;
import com.ledger.domain.accountheading.AccountHeadingId;
import com.ledger.domain.accountheading.AccountHeadingRepository;
import com.ledger.domain.accountheading.AccountNature;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAccountHeadingRepository
        implements AccountHeadingRepository {

    private final Map<AccountHeadingId, AccountHeading> headings =
            new ConcurrentHashMap<>();

    @Override
    public boolean existsByCode(String code) {
        return headings.values()
                .stream()
                .anyMatch(heading ->
                        heading.getCode().equals(code)
                );
    }

    @Override
    public boolean existsByCodeAndIdNot(
            String code,
            AccountHeadingId id
    ) {
        return headings.values()
                .stream()
                .anyMatch(heading ->
                        heading.getCode().equals(code)
                                && !heading.getId().equals(id)
                );
    }

    @Override
    public Optional<AccountHeading> findById(
            AccountHeadingId id
    ) {
        return Optional.ofNullable(
                headings.get(id)
        );
    }

    @Override
    public Optional<AccountHeading> findByCode(
            String code
    ) {
        return headings.values()
                .stream()
                .filter(heading ->
                        heading.getCode().equals(code)
                )
                .findFirst();
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

        return headings.values()
                .stream()
                .filter(heading ->
                        code == null
                                || heading.getCode().equals(code)
                )
                .filter(heading ->
                        name == null
                                || heading.getName()
                                .toLowerCase()
                                .contains(name.toLowerCase())
                )
                .filter(heading ->
                        parentId == null
                                || parentId.equals(
                                        heading.getParentId()
                                )
                )
                .filter(heading ->
                        level == null
                                || heading.getLevel() == level
                )
                .filter(heading ->
                        leaf == null
                                || heading.isLeaf() == leaf
                )
                .filter(heading ->
                        nature == null
                                || nature.equals(
                                        heading.getNature()
                                )
                )
                .toList();
    }

    @Override
    public void save(
            AccountHeading accountHeading
    ) {
        headings.put(
                accountHeading.getId(),
                accountHeading
        );
    }

    @Override
    public boolean hasChildren(
            AccountHeadingId accountHeadingId
    ) {
        return headings.values()
                .stream()
                .anyMatch(heading ->
                        accountHeadingId.equals(
                                heading.getParentId()
                        )
                );
    }

    @Override
    public boolean hasAccounts(
            AccountHeadingId accountHeadingId
    ) {
        // فعلاً Account هنوز پیاده‌سازی نشده
        return false;
    }

    @Override
    public void delete(
            AccountHeading accountHeading
    ) {
        headings.remove(
                accountHeading.getId()
        );
    }
}