package com.iitsaii.photobooth.domain.photo.repository;

import com.iitsaii.photobooth.domain.photo.entity.CapturedPhoto;
import com.iitsaii.photobooth.domain.session.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CapturedPhotoRepository extends JpaRepository<CapturedPhoto, Long> {
    List<CapturedPhoto> findBySessionOrderByShotNumber(Session session);

    int countBySession(Session session);

    boolean existsBySessionAndShotNumber(Session session, Integer shotNumber);

    List<CapturedPhoto> findAllBySessionAndIdIn(Session session, List<Long> photoIds);
}
