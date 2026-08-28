package com.iitsaii.photobooth.domain.printJob.repository;

import com.iitsaii.photobooth.domain.printJob.entity.PrintJob;
import com.iitsaii.photobooth.domain.printJob.entity.PrintJobStatus;
import com.iitsaii.photobooth.domain.session.entity.Session;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrintJobRepository extends JpaRepository<PrintJob, Long> {
    Optional<PrintJob> findBySession(Session session);

    Optional<PrintJob> findFirstByStatusOrderByCreatedAtAsc(PrintJobStatus printJobStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM PrintJob p
        WHERE p.status = :status
        ORDER BY p.createdAt ASC
        LIMIT 1
    """)
    Optional<PrintJob> findFirstQueuedForUpdate(@Param("status") PrintJobStatus status);
}
