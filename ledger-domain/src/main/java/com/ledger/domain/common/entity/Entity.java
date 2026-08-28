package com.ledger.domain.common.entity;

import java.util.Objects;

/**
 * Base class for all Domain Entities.
 *
 * @param <ID> the type of the entity identity
 */
public abstract class Entity<ID> {

    private final ID id;

    protected Entity(ID id) {
        this.id = Objects.requireNonNull(id, "Entity id must not be null");
    }

    public ID getId() {
        return id;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Entity<?> entity = (Entity<?>) o;
        return id.equals(entity.id);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), id);
    }
}