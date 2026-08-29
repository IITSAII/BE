package com.iitsaii.photobooth.domain.partner.service;

import com.iitsaii.photobooth.domain.partner.entity.Partner;
import com.iitsaii.photobooth.domain.partner.error.PartnerErrorCode;
import com.iitsaii.photobooth.domain.partner.repository.PartnerRepository;
import com.iitsaii.photobooth.global.error.CustomException;
import java.util.List;
import java.util.Objects;
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
     * 활성 업체 중 최근 당첨 순번이 가장 작은(=가장 오래전에 당첨됐거나 한 번도 당첨된 적 없는) 업체들을 후보로 삼아
     * 그중 하나를 무작위로 뽑고, 당첨 순번을 갱신한다.
     */
    @Transactional
    public Partner assignRandomPartner() {
        List<Partner> candidates = partnerRepository.findByActiveTrueOrderByLastAssignedSeqAsc();
        if (candidates.isEmpty()) {
            throw new CustomException(PartnerErrorCode.NO_ACTIVE_PARTNER);
        }

        Partner selected = pickLeastRecentlyAssigned(candidates);
        selected.assignNow(nextAssignedSeq());
        return selected;
    }

    /** candidatesSortedAsc는 lastAssignedSeq 오름차순으로 정렬되어 있다고 가정한다. */
    private Partner pickLeastRecentlyAssigned(List<Partner> candidatesSortedAsc) {
        Long minSeq = candidatesSortedAsc.get(0).getLastAssignedSeq();
        List<Partner> tied = candidatesSortedAsc.stream()
                .takeWhile(partner -> Objects.equals(partner.getLastAssignedSeq(), minSeq))
                .toList();
        return tied.get(ThreadLocalRandom.current().nextInt(tied.size()));
    }

    private long nextAssignedSeq() {
        Long maxSeq = partnerRepository.findMaxAssignedSeq();
        return maxSeq == null ? 1L : maxSeq + 1;
    }
}
