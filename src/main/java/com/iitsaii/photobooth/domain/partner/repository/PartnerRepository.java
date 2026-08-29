package com.iitsaii.photobooth.domain.partner.repository;

import com.iitsaii.photobooth.domain.partner.entity.Partner;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long> {

    /**
     * 활성 업체를 최근 당첨 순번(오름차순)으로 조회한다.
     * MySQL에서 NULL은 오름차순 정렬 시 가장 앞에 오므로, 한 번도 당첨되지 않은 업체가 최우선으로 조회된다.
     */
    List<Partner> findByActiveTrueOrderByLastAssignedSeqAsc();

    /** 다음 당첨 순번 발급을 위해 현재까지 부여된 최대 순번을 조회한다. 아무도 당첨된 적 없으면 null. */
    @Query("SELECT MAX(p.lastAssignedSeq) FROM Partner p")
    Long findMaxAssignedSeq();
}
