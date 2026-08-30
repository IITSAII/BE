package com.iitsaii.photobooth.domain.partner.repository;

import com.iitsaii.photobooth.domain.partner.entity.Partner;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long> {

    /**
     * 활성 업체를 최근 당첨 순번(오름차순, NULL 우선)으로 조회한다.
     * PostgreSQL은 ASC 정렬 시 NULL이 기본적으로 맨 뒤에 오므로 NULLS FIRST를 명시해야
     * 한 번도 당첨되지 않은 업체가 맨 앞에 오고, 가장 최근 당첨된 업체가 항상 맨 뒤에 오는 것을 보장한다.
     */
    @Query("SELECT p FROM Partner p WHERE p.active = true ORDER BY p.lastAssignedSeq ASC NULLS FIRST")
    List<Partner> findByActiveTrueOrderByLastAssignedSeqAsc();

    /** 다음 당첨 순번 발급을 위해 현재까지 부여된 최대 순번을 조회한다. 아무도 당첨된 적 없으면 null. */
    @Query("SELECT MAX(p.lastAssignedSeq) FROM Partner p")
    Long findMaxAssignedSeq();
}
