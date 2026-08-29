package com.iitsaii.photobooth.domain.partner.service;

import com.iitsaii.photobooth.domain.partner.entity.Partner;
import com.iitsaii.photobooth.domain.partner.error.PartnerErrorCode;
import com.iitsaii.photobooth.domain.partner.repository.PartnerRepository;
import com.iitsaii.photobooth.global.error.CustomException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartnerService {

    private final PartnerRepository partnerRepository;

    /**
     * 활성 업체 중 가장 최근에 당첨된 업체 1곳만 후보에서 제외하고, 나머지 중 하나를 무작위로 뽑아
     * 당첨 순번을 갱신한다. 특정 업체가 연속으로 당첨되는 상황을 최소화하는 게 목적이다.
     */
    @Transactional
    public Partner assignRandomPartner() {
        List<Partner> activePartners = partnerRepository.findByActiveTrueOrderByLastAssignedSeqAsc();
        if (activePartners.isEmpty()) {
            throw new CustomException(PartnerErrorCode.NO_ACTIVE_PARTNER);
        }

        List<Partner> candidates = excludeMostRecentlyAssigned(activePartners);
        Partner selected = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        selected.assignNow(nextAssignedSeq());
        return selected;
    }

    public Partner getById(Long partnerId) {
        return partnerRepository.findById(partnerId)
                .orElseThrow(() -> new CustomException(PartnerErrorCode.PARTNER_NOT_FOUND));
    }

    /** 매거진 페이지 노출용 활성 업체 목록. */
    public List<Partner> getActivePartners() {
        return partnerRepository.findByActiveTrueOrderByLastAssignedSeqAsc();
    }

    /**
     * activePartnersSortedAsc는 lastAssignedSeq 오름차순(가장 최근 당첨 업체가 맨 뒤)으로 정렬되어 있다고 가정한다.
     * 활성 업체가 1곳뿐이면 제외할 수 없으므로 그대로 반환하고,
     * 아무도 당첨된 적 없으면(맨 뒤 업체의 seq도 null) 제외할 "최근 당첨 업체"가 없으므로 그대로 반환한다.
     */
    private List<Partner> excludeMostRecentlyAssigned(List<Partner> activePartnersSortedAsc) {
        if (activePartnersSortedAsc.size() <= 1) {
            return activePartnersSortedAsc;
        }

        Partner mostRecentlyAssigned = activePartnersSortedAsc.get(activePartnersSortedAsc.size() - 1);
        if (mostRecentlyAssigned.getLastAssignedSeq() == null) {
            return activePartnersSortedAsc;
        }

        return activePartnersSortedAsc.stream()
                .filter(partner -> partner != mostRecentlyAssigned)
                .toList();
    }

    private long nextAssignedSeq() {
        Long maxSeq = partnerRepository.findMaxAssignedSeq();
        return maxSeq == null ? 1L : maxSeq + 1;
    }
}
