package com.iitsaii.photobooth.domain.partner.repository;

import com.iitsaii.photobooth.domain.partner.entity.Partner;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long> {

    /**
     * 노출 가능한(활성이면서 협약이 만료되지 않은) 업체를 최근 당첨 순번(오름차순, NULL 우선)으로 조회한다.
     * isActive는 관리자가 즉시 수동으로 켜고 끄는 값이고, contractEndDate는 정해진 날짜에 자동으로
     * 만료되는 조건이라 서로 다른 축이다 - 노출 여부는 두 조건을 모두 만족해야 한다.
     * PostgreSQL은 ASC 정렬 시 NULL이 기본적으로 맨 뒤에 오므로 NULLS FIRST를 명시해야
     * 한 번도 당첨되지 않은 업체가 맨 앞에 오고, 가장 최근 당첨된 업체가 항상 맨 뒤에 오는 것을 보장한다.
     */
    @Query("SELECT p FROM Partner p WHERE p.active = true "
            + "AND (p.contractEndDate IS NULL OR p.contractEndDate >= CURRENT_DATE) "
            + "ORDER BY p.lastAssignedSeq ASC NULLS FIRST")
    List<Partner> findAvailableOrderByLastAssignedSeqAsc();

    /** 다음 당첨 순번 발급을 위해 현재까지 부여된 최대 순번을 조회한다. 아무도 당첨된 적 없으면 null. */
    @Query("SELECT MAX(p.lastAssignedSeq) FROM Partner p")
    Long findMaxAssignedSeq();
}
