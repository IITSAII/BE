package com.iitsaii.photobooth.domain.session.repository;

import com.iitsaii.photobooth.domain.session.entity.Session;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findBySessionId(String sessionId);
}
