package com.ledger.test.domain.accountheading;

import org.junit.jupiter.api.Test;

import com.ledger.domain.accountheading.AccountHeading;
import com.ledger.domain.accountheading.AccountHeadingId;
import com.ledger.domain.accountheading.AccountNature;

import static org.junit.jupiter.api.Assertions.*;

class AccountHeadingTest {

    // =========================================================
    // Root
    // =========================================================

    @Test
    void shouldCreateRootAccountHeading() {

        AccountHeading heading = AccountHeading.createRoot(
                new AccountHeadingId("AH-ROOT"),
                "100000000",
                "روت"
        );

        assertEquals(
                new AccountHeadingId("AH-ROOT"),
                heading.getId()
        );

        assertNull(heading.getParentId());
        assertEquals("100000000", heading.getCode());
        assertEquals("روت", heading.getName());

        assertNull(heading.getNature());
        assertFalse(heading.isAllowNegativeBalance());
        assertFalse(heading.isLeaf());
        assertEquals(0, heading.getLevel());

        assertTrue(heading.isRoot());
    }

    @Test
    void rootShouldHaveNoParent() {

        AccountHeading root = createRoot();

        assertNull(root.getParentId());
    }

    @Test
    void rootShouldHaveLevelZero() {

        AccountHeading root = createRoot();

        assertEquals(0, root.getLevel());
    }

    @Test
    void rootShouldNotBeLeaf() {

        AccountHeading root = createRoot();

        assertFalse(root.isLeaf());
    }

    @Test
    void rootShouldNotHaveNature() {

        AccountHeading root = createRoot();

        assertNull(root.getNature());
    }

    @Test
    void rootShouldNotAllowNegativeBalance() {

        AccountHeading root = createRoot();

        assertFalse(root.isAllowNegativeBalance());
    }

    // =========================================================
    // First Level
    // =========================================================

    @Test
    void shouldCreateFirstLevelFromRoot() {

        AccountHeading root = createRoot();

        AccountHeading heading = root.createFirstLevel(
                new AccountHeadingId("AH-001"),
                "101000000",
                "بهکار",
                AccountNature.DEBIT,
                false
        );

        assertEquals(root.getId(), heading.getParentId());

        assertEquals("101000000", heading.getCode());
        assertEquals("بهکار", heading.getName());

        assertEquals(
                AccountNature.DEBIT,
                heading.getNature()
        );

        assertFalse(heading.isAllowNegativeBalance());

        assertEquals(1, heading.getLevel());
        assertFalse(heading.isLeaf());
        assertFalse(heading.isRoot());
    }

    @Test
    void firstLevelShouldHaveLevelOne() {

        AccountHeading heading = createDebitFirstLevel();

        assertEquals(1, heading.getLevel());
    }

    @Test
    void onlyRootShouldCreateFirstLevel() {

        AccountHeading firstLevel = createDebitFirstLevel();

        assertThrows(
                IllegalStateException.class,
                () -> firstLevel.createFirstLevel(
                        new AccountHeadingId("AH-002"),
                        "101010000",
                        "سطح دوم",
                        AccountNature.DEBIT,
                        false
                )
        );
    }

    @Test
    void firstLevelShouldRequireNature() {

        AccountHeading root = createRoot();

        assertThrows(
                NullPointerException.class,
                () -> root.createFirstLevel(
                        new AccountHeadingId("AH-001"),
                        "101000000",
                        "بهکار",
                        null,
                        false
                )
        );
    }

    // =========================================================
    // Child
    // =========================================================

    @Test
    void shouldCreateChildFromParent() {

        AccountHeading parent = createDebitFirstLevel();

        AccountHeading child = parent.createChild(
                new AccountHeadingId("AH-002"),
                "101010000",
                "بهکار/اول"
        );

        assertEquals(
                parent.getId(),
                child.getParentId()
        );

        assertEquals(2, child.getLevel());

        assertEquals(
                parent.getLevel() + 1,
                child.getLevel()
        );

        assertFalse(child.isLeaf());
    }

    @Test
    void parentShouldBeAbleToCreateMultipleChildren() {

        AccountHeading parent = createDebitFirstLevel();

        AccountHeading childOne = parent.createChild(
                new AccountHeadingId("AH-002"),
                "101010000",
                "بهکار/اول"
        );

        AccountHeading childTwo = parent.createChild(
                new AccountHeadingId("AH-003"),
                "101020000",
                "بهکار/دوم"
        );

        assertEquals(parent.getId(), childOne.getParentId());
        assertEquals(parent.getId(), childTwo.getParentId());

        assertNotEquals(
                childOne.getId(),
                childTwo.getId()
        );

        assertEquals(
                childOne.getLevel(),
                childTwo.getLevel()
        );
    }

    @Test
    void childLevelShouldAlwaysBeParentLevelPlusOne() {

        AccountHeading levelOne = createDebitFirstLevel();

        AccountHeading levelTwo = levelOne.createChild(
                new AccountHeadingId("AH-002"),
                "101010000",
                "سطح دوم"
        );

        AccountHeading levelThree = levelTwo.createChild(
                new AccountHeadingId("AH-003"),
                "101010100",
                "سطح سوم"
        );

        assertEquals(
                levelOne.getLevel() + 1,
                levelTwo.getLevel()
        );

        assertEquals(
                levelTwo.getLevel() + 1,
                levelThree.getLevel()
        );
    }

    // =========================================================
    // Inheritance
    // =========================================================

    @Test
    void childShouldInheritNatureFromParent() {

        AccountHeading parent = createDebitFirstLevel();

        AccountHeading child = parent.createChild(
                new AccountHeadingId("AH-002"),
                "101010000",
                "بهکار/اول"
        );

        assertEquals(
                AccountNature.DEBIT,
                child.getNature()
        );
    }

    @Test
    void childShouldInheritCreditNatureFromParent() {

        AccountHeading parent = createCreditFirstLevel();

        AccountHeading child = parent.createChild(
                new AccountHeadingId("AH-002"),
                "102010000",
                "بستانکار/اول"
        );

        assertEquals(
                AccountNature.CREDIT,
                child.getNature()
        );
    }

    @Test
    void childShouldInheritNegativeBalancePolicyFromParent() {

        AccountHeading parent = createFirstLevel(true);

        AccountHeading child = parent.createChild(
                new AccountHeadingId("AH-002"),
                "101010000",
                "بهکار/اول"
        );

        assertTrue(
                child.isAllowNegativeBalance()
        );
    }

    @Test
    void childShouldInheritDisallowedNegativeBalanceFromParent() {

        AccountHeading parent = createDebitFirstLevel();

        AccountHeading child = parent.createChild(
                new AccountHeadingId("AH-002"),
                "101010000",
                "بهکار/اول"
        );

        assertFalse(
                child.isAllowNegativeBalance()
        );
    }

    // =========================================================
    // Level & Leaf
    // =========================================================

    @Test
    void levelFiveShouldBeLeaf() {

        AccountHeading heading = createLevelFive();

        assertEquals(5, heading.getLevel());
        assertTrue(heading.isLeaf());
    }

    @Test
    void levelBelowFiveShouldNotBeLeaf() {

        AccountHeading heading = createLevelFour();

        assertEquals(4, heading.getLevel());
        assertFalse(heading.isLeaf());
    }

    @Test
    void levelFiveShouldNotAllowAnotherHeadingChild() {

        AccountHeading levelFive = createLevelFive();

        assertThrows(
                IllegalStateException.class,
                () -> levelFive.createChild(
                        new AccountHeadingId("AH-006"),
                        "101010112",
                        "سطح ششم"
                )
        );
    }

    // =========================================================
    // Rename
    // =========================================================

    @Test
    void shouldRenameAccountHeading() {

        AccountHeading original = createDebitFirstLevel();

        AccountHeading renamed = original.rename("نام جدید");

        assertEquals(
                "نام جدید",
                renamed.getName()
        );

        assertEquals(
                original.getId(),
                renamed.getId()
        );

        assertEquals(
                original.getParentId(),
                renamed.getParentId()
        );

        assertEquals(
                original.getCode(),
                renamed.getCode()
        );

        assertEquals(
                original.getNature(),
                renamed.getNature()
        );

        assertEquals(
                original.isAllowNegativeBalance(),
                renamed.isAllowNegativeBalance()
        );

        assertEquals(
                original.getLevel(),
                renamed.getLevel()
        );

        assertEquals(
                original.isLeaf(),
                renamed.isLeaf()
        );
    }

    // =========================================================
    // Validation
    // =========================================================

    @Test
    void shouldRejectNullId() {

        assertThrows(
                NullPointerException.class,
                () -> AccountHeading.createRoot(
                        null,
                        "100000000",
                        "روت"
                )
        );
    }

    @Test
    void shouldRejectNullCode() {

        assertThrows(
                NullPointerException.class,
                () -> AccountHeading.createRoot(
                        new AccountHeadingId("AH-ROOT"),
                        null,
                        "روت"
                )
        );
    }

    @Test
    void shouldRejectBlankCode() {

        assertThrows(
                IllegalArgumentException.class,
                () -> AccountHeading.createRoot(
                        new AccountHeadingId("AH-ROOT"),
                        " ",
                        "روت"
                )
        );
    }

    @Test
    void shouldRejectEmptyCode() {

        assertThrows(
                IllegalArgumentException.class,
                () -> AccountHeading.createRoot(
                        new AccountHeadingId("AH-ROOT"),
                        "",
                        "روت"
                )
        );
    }

    @Test
    void shouldRejectNullName() {

        assertThrows(
                NullPointerException.class,
                () -> AccountHeading.createRoot(
                        new AccountHeadingId("AH-ROOT"),
                        "100000000",
                        null
                )
        );
    }

    @Test
    void shouldRejectBlankName() {

        assertThrows(
                IllegalArgumentException.class,
                () -> AccountHeading.createRoot(
                        new AccountHeadingId("AH-ROOT"),
                        "100000000",
                        " "
                )
        );
    }

    @Test
    void shouldRejectEmptyName() {

        assertThrows(
                IllegalArgumentException.class,
                () -> AccountHeading.createRoot(
                        new AccountHeadingId("AH-ROOT"),
                        "100000000",
                        ""
                )
        );
    }

    // =========================================================
    // Helpers
    // =========================================================

    private AccountHeading createRoot() {

        return AccountHeading.createRoot(
                new AccountHeadingId("AH-ROOT"),
                "100000000",
                "روت"
        );
    }

    private AccountHeading createFirstLevel(
            boolean allowNegativeBalance
    ) {

        return createRoot().createFirstLevel(
                new AccountHeadingId("AH-001"),
                "101000000",
                "بهکار",
                AccountNature.DEBIT,
                allowNegativeBalance
        );
    }

    private AccountHeading createDebitFirstLevel() {

        return createFirstLevel(false);
    }

    private AccountHeading createCreditFirstLevel() {

        return createRoot().createFirstLevel(
                new AccountHeadingId("AH-001"),
                "102000000",
                "بستانکار",
                AccountNature.CREDIT,
                false
        );
    }

    private AccountHeading createLevelTwo() {

        return createDebitFirstLevel().createChild(
                new AccountHeadingId("AH-002"),
                "101010000",
                "بهکار/اول"
        );
    }

    private AccountHeading createLevelThree() {

        return createLevelTwo().createChild(
                new AccountHeadingId("AH-003"),
                "101010100",
                "بهکار/اول/دوم"
        );
    }

    private AccountHeading createLevelFour() {

        return createLevelThree().createChild(
                new AccountHeadingId("AH-004"),
                "101010110",
                "بهکار چهارم"
        );
    }

    private AccountHeading createLevelFive() {

        return createLevelFour().createChild(
                new AccountHeadingId("AH-005"),
                "101010111",
                "بهکار پنجم"
        );
    }
}