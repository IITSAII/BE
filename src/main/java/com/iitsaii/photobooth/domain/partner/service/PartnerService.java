package com.iitsaii.photobooth.domain.partner.service;

import com.iitsaii.photobooth.domain.partner.entity.Partner;
import com.iitsaii.photobooth.domain.partner.error.PartnerErrorCode;
import com.iitsaii.photobooth.domain.partner.repository.PartnerRepository;
import com.iitsaii.photobooth.domain.session.entity.Session;
import com.iitsaii.photobooth.global.error.CustomException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartnerService {

    private final PartnerRepository partnerRepository;

    /** 영업 중인 업체가 하나도 없을 때 대신 배정할 업체 이름. */
    private static final Set<String> FALLBACK_PARTNER_NAMES = Set.of("피치못한", "반짝");

    /**
     * 다른 업체와 영업시간이 겹치는 동안은 후순위로 미루고 싶은 업체 이름. 이 업체(들)는 영업시간
     * 자체는 실제 영업일(예: 월~금)을 그대로 갖고 있지만, 같은 시각에 이 목록에 없는 업체가 하나라도
     * 영업 중이면 배정 비율에 LOW_PRIORITY_PENALTY만큼 페널티를 더해 왠만하면 뽑히지 않게 한다.
     * 완전히 배제하는 건 아니라서, 다른 업체들이 유난히 자주 뽑혀 비율이 크게 오르면 그때는 이
     * 업체(들)도 뽑힐 수 있다. 이 업체(들)만 영업 중인 시간대(다른 업체가 전부 휴무인 요일)에는
     * 페널티 없이 정상적으로 후보가 된다.
     */
    private static final Set<String> LOW_PRIORITY_PARTNER_NAMES = Set.of("피치못한");

    /**
     * LOW_PRIORITY_PARTNER_NAMES에 적용하는 배정 비율 페널티. assignmentRatio()는 항상 [0, 1]
     * 범위라, 다른 업체(들)의 비율이 (그 업체의 실제 비율 + 이 페널티)보다 높아야만 역전되어
     * 뽑힐 수 있다. 값을 키울수록 더 드물게 뽑히고, 줄일수록 더 자주 뽑힌다.
     */
    private static final double LOW_PRIORITY_PENALTY = 0.5;

    /**
     * 세션에 업체를 배정한다 (결제 승인 직후, 또는 그때 실패한 세션의 재시도 조회 시점).
     * 활성 업체가 없어 배정에 실패해도 예외를 던지지 않고 로그만 남긴다 - 결제는 이미 외부에서
     * 승인되어 되돌릴 수 없으므로, 배정 실패로 호출부의 흐름(결제 확정, 세션 조회)을 막지 않기 위함.
     * 동시에 여러 세션이 배정을 시도해 Partner의 assignedCount/eligibleCount 갱신이 충돌해도
     * (@Version 낙관적 락) 같은 방식으로 실패 처리하고 넘어간다 - 별도 재시도는 하지 않는다.
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
            // fallback(피치못한/반짝)까지 실패했다는 뜻 - 활성 업체 자체가 없거나 fallback 이름이
            // 잘못됐다는 신호라 흔한 상황이 아니다. 즉시 알아챌 수 있도록 error로 남긴다.
            log.error("제휴 업체 배정에 실패했습니다. 수동 배정 검토 필요. sessionId={}, errorCode={}",
                    session.getSessionId(), e.getErrorCode(), e);
            return false;
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("제휴 업체 배정 중 동시 갱신 충돌이 발생했습니다. 수동 배정 검토 필요. sessionId={}",
                    session.getSessionId(), e);
            return false;
        }
    }

    /**
     * 노출 가능한(활성이면서 협약이 만료되지 않은) 업체 중 지금 이 시각에 실제로 영업 중인 업체만
     * 후보로 추리고, 그중 배정 비율(assignedCount / eligibleCount)이 가장 낮은 업체(들)를 우선한다.
     * 동률이면 가장 최근에 당첨된 업체 1곳만 제외하고 나머지 중 무작위로 뽑는다.
     *
     * 리셋 없이 계속 누적되는 비율을 쓰는 이유: 특정 요일에만 영업하는 업체가 그 요일을 독점해서
     * 배정 횟수가 일시적으로 몰려도, eligibleCount도 함께 커지므로 비율 자체는 자연히 낮아지지
     * 않는다. 반대로 영업일이 적어 기회 자체가 적었던 업체는 비율이 낮게 유지되어, 다음 공통
     * 영업일에 자동으로 우선권을 갖게 된다. 주기적으로 리셋하면 이런 자기 교정이 매번 사라지므로
     * 리셋하지 않는다.
     *
     * 영업 중인 업체가 하나도 없으면 FALLBACK_PARTNER_NAMES(피치못한, 반짝) 중 활성 상태인
     * 업체만 후보로 대신 사용한다 (영업시간 외에도 배정 자체는 막히지 않도록 하는 정책).
     *
     * 영업 중인 업체가 있으면, LOW_PRIORITY_PARTNER_NAMES(피치못한)도 다른 업체와 함께 후보
     * 풀에는 남되(recordEligible은 정상적으로 쌓인다), selectLowestRatio에서 비율 페널티를 받아
     * 왠만하면 뽑히지 않는다 - 다른 업체와 영업일이 겹치는 요일(화~목 등)에 피치못한이 배정
     * 기회를 과도하게 가져가지 않도록 하기 위함. 피치못한만 영업 중인 요일(다른 업체가 전부
     * 휴무)에는 페널티 없이 정상적으로 후보가 된다.
     */
    @Transactional
    public Partner assignRandomPartner() {
        return assignRandomPartner(LocalDateTime.now());
    }

    Partner assignRandomPartner(LocalDateTime now) {
        List<Partner> availablePartners = partnerRepository.findAvailableOrderByLastAssignedSeqAsc();
        List<Partner> operatingPartners = availablePartners.stream()
                .filter(partner -> partner.isOperatingAt(now))
                .toList();
        List<Partner> candidatePool = operatingPartners.isEmpty()
                ? availablePartners.stream().filter(partner -> FALLBACK_PARTNER_NAMES.contains(partner.getName())).toList()
                : operatingPartners;

        if (operatingPartners.isEmpty() && candidatePool.size() < FALLBACK_PARTNER_NAMES.size()) {
            // FALLBACK_PARTNER_NAMES의 업체명이 DB의 실제 이름과 어긋났거나 일부가 비활성/만료된 경우.
            // 배정 자체는 계속 진행하되(가능한 만큼은 배정), 설정이 어긋났다는 걸 바로 알 수 있도록 남긴다.
            log.warn("fallback 배정 후보 중 일부를 찾지 못했습니다. expected={}, found={}",
                    FALLBACK_PARTNER_NAMES, candidatePool.stream().map(Partner::getName).toList());
        }
        if (candidatePool.isEmpty()) {
            throw new CustomException(PartnerErrorCode.NO_ACTIVE_PARTNER);
        }

        // 후보 선정은 반드시 eligibleCount를 올리기 전, 기존 누적 비율로 해야 한다.
        // 먼저 전부 올려버리면 분모가 다 같이 커져서 비율 순서 자체가 바뀔 수 있다
        // (예: A=10/11, B=1/1이면 A가 더 낮지만, 먼저 +1하면 A=10/12, B=1/2로 B가 더 낮아짐).
        List<Partner> lowestRatioPartners = selectLowestRatio(candidatePool);
        List<Partner> candidates = excludeMostRecentlyAssigned(lowestRatioPartners);
        Partner selected = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));

        candidatePool.forEach(Partner::recordEligible);
        selected.assignNow(nextAssignedSeq());
        selected.recordAssigned();
        return selected;
    }

    /**
     * candidatePool 중 실질 배정 비율(effectiveRatio)이 가장 낮은 업체(들)만 남긴다.
     * LOW_PRIORITY_PARTNER_NAMES가 아닌 업체가 후보 풀에 하나라도 섞여 있으면, LOW_PRIORITY
     * 업체의 비율에 페널티를 더해 계산한다 (완전 배제는 아니라서, 다른 업체들의 비율이 충분히
     * 높아지면 그래도 역전되어 뽑힐 수 있다).
     */
    private List<Partner> selectLowestRatio(List<Partner> candidatePool) {
        boolean applyLowPriorityPenalty = candidatePool.stream()
                .anyMatch(partner -> !LOW_PRIORITY_PARTNER_NAMES.contains(partner.getName()));

        double minEffectiveRatio = candidatePool.stream()
                .mapToDouble(partner -> effectiveRatio(partner, applyLowPriorityPenalty))
                .min()
                .orElse(0.0);

        return candidatePool.stream()
                .filter(partner -> effectiveRatio(partner, applyLowPriorityPenalty) == minEffectiveRatio)
                .toList();
    }

    private double effectiveRatio(Partner partner, boolean applyLowPriorityPenalty) {
        double ratio = partner.assignmentRatio();
        if (applyLowPriorityPenalty && LOW_PRIORITY_PARTNER_NAMES.contains(partner.getName())) {
            return ratio + LOW_PRIORITY_PENALTY;
        }
        return ratio;
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
