package com.ledger.domain.accountheading;

import com.ledger.domain.common.aggregate.AggregateRoot;

import java.util.Objects;

// ---------------------------------------------------------
// Identity
// ---------------------------------------------------------
public class AccountHeading extends AggregateRoot<AccountHeadingId> {

    private static final int ROOT_LEVEL = 0;
    private static final int MAX_LEVEL = 5;

    // ---------------------------------------------------------
    // State
    // ---------------------------------------------------------

    private final AccountHeadingId parentId;
    private final String code;
    private final String name;
    private final AccountNature nature;
    private final boolean allowNegativeBalance;
    private final int level;

    private AccountHeading(
            AccountHeadingId id,
            AccountHeadingId parentId,
            String code,
            String name,
            AccountNature nature,
            boolean allowNegativeBalance,
            int level
    ) {

        super(Objects.requireNonNull(id, "id must not be null"));

        this.parentId = parentId;
        this.code = validateCode(code);
        this.name = validateName(name);
        this.nature = nature;
        this.allowNegativeBalance = allowNegativeBalance;
        this.level = validateLevel(level);
        
    }

    // ---------------------------------------------------------
    // Behavior
    // ---------------------------------------------------------

    
    public static AccountHeading createRoot(
        AccountHeadingId id,
        String code,
        String name
    ) {

        return new AccountHeading(
                id,
                null,
                code,
                name,
                null,
                false,
                ROOT_LEVEL
        );
    }

    public AccountHeading createFirstLevel(
        AccountHeadingId id,
        String code,
        String name,
        AccountNature nature,
        boolean allowNegativeBalance
    ) {

        if (!isRoot()) {
            throw new IllegalStateException(
                    "Only root AccountHeading can create first level"
            );
        }

        Objects.requireNonNull(
                nature,
                "nature must not be null"
        );

        return new AccountHeading(
                id,
                this.getId(),
                code,
                name,
                nature,
                allowNegativeBalance,
                this.level + 1
        );
    }

    public AccountHeading createChild(
            AccountHeadingId id,
            String code,
            String name
    ) {

        if (isLeaf()) {
            throw new IllegalStateException(
                    "Level 5 AccountHeading cannot have child headings"
            );
        }

        return new AccountHeading(
                id,
                this.getId(),
                code,
                name,
                this.nature,
                this.allowNegativeBalance,
                this.level + 1
        );
    }

    public AccountHeading rename(String newName) {

        return new AccountHeading(
                this.getId(),
                this.parentId,
                this.code,
                newName,
                this.nature,
                this.allowNegativeBalance,
                this.level
        );
    }

    ///Query / Derived Behavior  no Command Behavior
    public boolean isLeaf() {
        return level == MAX_LEVEL;
    }
    
    public boolean isRoot() {
        return level == ROOT_LEVEL;
    }
    // ---------------------------------------------------------
    // State Accessors
    // ---------------------------------------------------------

    public AccountHeadingId getParentId() {
        return parentId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public AccountNature getNature() {
        return nature;
    }

    public boolean isAllowNegativeBalance() {
        return allowNegativeBalance;
    }

    public int getLevel() {
        return level;
    }

   

    // ---------------------------------------------------------
    // Invariants
    // ---------------------------------------------------------

    private static String validateCode(String code) {

        Objects.requireNonNull(
                code,
                "code must not be null"
        );

        if (code.isBlank()) {
            throw new IllegalArgumentException(
                    "code must not be blank"
            );
        }

        return code;
    }

    private static String validateName(String name) {

        Objects.requireNonNull(
                name,
                "name must not be null"
        );

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "name must not be blank"
            );
        }

        return name;
    }

    private static int validateLevel(int level) {

        if (level < ROOT_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException(
                    "AccountHeading level must be between "
                            + ROOT_LEVEL
                            + " and "
                            + MAX_LEVEL
            );
        }

        return level;
    }
}