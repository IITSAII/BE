package com.iitsaii.photobooth.domain.partner.service;

import com.iitsaii.photobooth.domain.partner.entity.Partner;
import com.iitsaii.photobooth.domain.partner.error.PartnerErrorCode;
import com.iitsaii.photobooth.domain.partner.repository.PartnerRepository;
import com.iitsaii.photobooth.domain.session.entity.Session;
import com.iitsaii.photobooth.global.error.CustomException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartnerService {

    private final PartnerRepository partnerRepository;

    /**
     * 세션에 업체를 배정한다 (결제 승인 직후, 또는 그때 실패한 세션의 재시도 조회 시점).
     * 활성 업체가 없어 배정에 실패해도 예외를 던지지 않고 로그만 남긴다 - 결제는 이미 외부에서
     * 승인되어 되돌릴 수 없으므로, 배정 실패로 호출부의 흐름(결제 확정, 세션 조회)을 막지 않기 위함.
     * 성공하면 true, 배정 가능한 업체가 없어 실패하면 false를 반환한다.
     */
    @Transactional
    public boolean assignPartnerToSession(Session session) {
        try {
            Partner partner = assignRandomPartner();
            LocalDateTime couponExpiresAt = LocalDateTime.now().toLocalDate().plusDays(1).atTime(23, 59, 59);
            session.assignPartner(partner.getId(), couponExpiresAt);
            return true;
        } catch (CustomException e) {
            log.warn("제휴 업체 배정에 실패했습니다. 수동 배정 검토 필요. sessionId={}, errorCode={}",
                    session.getSessionId(), e.getErrorCode(), e);
            return false;
        }
    }

    /**
     * 노출 가능한(활성이면서 협약이 만료되지 않은) 업체 중 가장 최근에 당첨된 업체 1곳만 후보에서
     * 제외하고, 나머지 중 하나를 무작위로 뽑아 당첨 순번을 갱신한다.
     * 특정 업체가 연속으로 당첨되는 상황을 최소화하는 게 목적이다.
     */
    @Transactional
    public Partner assignRandomPartner() {
        List<Partner> availablePartners = partnerRepository.findAvailableOrderByLastAssignedSeqAsc();
        if (availablePartners.isEmpty()) {
            throw new CustomException(PartnerErrorCode.NO_ACTIVE_PARTNER);
        }

        List<Partner> candidates = excludeMostRecentlyAssigned(availablePartners);
        Partner selected = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        selected.assignNow(nextAssignedSeq());
        return selected;
    }

    public Partner getById(Long partnerId) {
        return partnerRepository.findById(partnerId)
                .orElseThrow(() -> new CustomException(PartnerErrorCode.PARTNER_NOT_FOUND));
    }

    /** 매거진 페이지 노출용 업체 목록 (활성이면서 협약이 만료되지 않은 업체만). */
    public List<Partner> getActivePartners() {
        return partnerRepository.findAvailableOrderByLastAssignedSeqAsc();
    }

    /**
     * availablePartnersSortedAsc는 lastAssignedSeq 오름차순(가장 최근 당첨 업체가 맨 뒤)으로 정렬되어 있다고 가정한다.
     * 후보가 1곳뿐이면 제외할 수 없으므로 그대로 반환하고,
     * 아무도 당첨된 적 없으면(맨 뒤 업체의 seq도 null) 제외할 "최근 당첨 업체"가 없으므로 그대로 반환한다.
     */
    private List<Partner> excludeMostRecentlyAssigned(List<Partner> availablePartnersSortedAsc) {
        if (availablePartnersSortedAsc.size() <= 1) {
            return availablePartnersSortedAsc;
        }

        Partner mostRecentlyAssigned = availablePartnersSortedAsc.get(availablePartnersSortedAsc.size() - 1);
        if (mostRecentlyAssigned.getLastAssignedSeq() == null) {
            return availablePartnersSortedAsc;
        }

        return availablePartnersSortedAsc.stream()
                .filter(partner -> partner != mostRecentlyAssigned)
                .toList();
    }

    private long nextAssignedSeq() {
        Long maxSeq = partnerRepository.findMaxAssignedSeq();
        return maxSeq == null ? 1L : maxSeq + 1;
    }
}
