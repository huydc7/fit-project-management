package com.example.quanlydetai.repository;

import com.example.quanlydetai.entity.ResultEmail;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface ResultEmailRepository extends JpaRepository<ResultEmail, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ResultEmail> findFirstByStateInAndNextAttemptAtLessThanEqualOrderByCreatedAtAscIdAsc(
            Collection<String> states, LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            """
            update ResultEmail e set e.state='FAILED'
            where e.state in ('PENDING','PROCESSING') and e.attempts >= :maxAttempts
            and e.nextAttemptAt <= :now
            """)
    int failExhausted(@Param("maxAttempts") int maxAttempts, @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from ResultEmail e where e.id=:id")
    Optional<ResultEmail> lockById(@Param("id") String id);

    @Query(
            """
            select e from ResultEmail e
            where (:state='' or e.state=:state)
            and (lower(e.topic) like :pattern escape '!'
              or lower(e.student) like :pattern escape '!'
              or lower(e.recipient) like :pattern escape '!')
            """)
    Page<ResultEmail> search(
            @Param("state") String state, @Param("pattern") String pattern, Pageable pageable);
}
