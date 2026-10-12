package com.example.quanlydetai.entity;

import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

/** Equality never traverses relationships; the stable hash also supports assigned IDs. */
public abstract class AbstractEntity {
    protected abstract Object entityId();

    private static Class<?> persistentClass(Object value) {
        return value instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : value.getClass();
    }

    private static Object identifier(AbstractEntity value) {
        return value instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getIdentifier()
                : value.entityId();
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AbstractEntity entity)
                || persistentClass(this) != persistentClass(other)) return false;
        Object id = identifier(this);
        return id != null && Objects.equals(id, identifier(entity));
    }

    @Override
    public final int hashCode() {
        return persistentClass(this).hashCode();
    }
}
