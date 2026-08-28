package com.iitsaii.photobooth.domain.printJob.repository;

import com.iitsaii.photobooth.domain.printJob.entity.PrintJob;
import com.iitsaii.photobooth.domain.printJob.entity.PrintJobStatus;
import com.iitsaii.photobooth.domain.session.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrintJobRepository extends JpaRepository<PrintJob, Long> {
    boolean existsBySession(Session session);

    Optional<PrintJob> findBySession(Session session);

    Optional<PrintJob> findFirstByStatusOrderByCreatedAtAsc(PrintJobStatus printJobStatus);
}
