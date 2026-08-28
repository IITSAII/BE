package com.iitsaii.photobooth.domain.photo.repository;

import com.iitsaii.photobooth.domain.photo.entity.SelectedPhoto;
import com.iitsaii.photobooth.domain.session.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SelectedPhotoRepository extends JpaRepository<SelectedPhoto, Long> {
    List<SelectedPhoto> findAllByCapturedPhoto_SessionOrderBySelectOrderAsc(Session session);

    boolean existsByCapturedPhoto_Session(Session session);
}
