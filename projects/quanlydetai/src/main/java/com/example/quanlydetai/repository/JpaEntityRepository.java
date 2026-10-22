package com.example.quanlydetai.repository;

import com.example.quanlydetai.exception.BusinessException;

import jakarta.persistence.*;

import java.util.*;

/**
 * JPA operations for cross-entity transactions. Call inside a service transaction. Inserts flush
 * and refresh database-trigger defaults; locks serialize competing writes.
 */
@org.springframework.stereotype.Repository
public class JpaEntityRepository {
    @PersistenceContext private EntityManager em;

    public <T> T find(Class<T> type, Object id) {
        return id == null ? null : em.find(type, id);
    }

    public void remove(Object entity) {
        em.remove(entity);
    }

    public <T> T require(Class<T> type, Object id) {
        T value = id == null ? null : em.find(type, id);
        if (value == null) throw new BusinessException("Không tìm thấy dữ liệu yêu cầu.");
        return value;
    }

    public <T> T lock(Class<T> type, Object id) {
        T value = id == null ? null : em.find(type, id, LockModeType.PESSIMISTIC_WRITE);
        if (value == null) throw new BusinessException("Không tìm thấy dữ liệu yêu cầu.");
        return value;
    }

    public <T> List<T> query(String jpql, Class<T> type, Object... parameters) {
        var query = em.createQuery(jpql, type);
        for (int i = 0; i < parameters.length; i++) query.setParameter(i + 1, parameters[i]);
        return query.getResultList();
    }

    public boolean exists(String jpql, Object... parameters) {
        var query = em.createQuery(jpql, Object.class);
        for (int i = 0; i < parameters.length; i++) query.setParameter(i + 1, parameters[i]);
        return !query.setMaxResults(1).getResultList().isEmpty();
    }

    public void insert(Object entity) {
        em.persist(entity);
        em.flush();
        em.refresh(entity);
    }

    public void flush() {
        em.flush();
    }
}
