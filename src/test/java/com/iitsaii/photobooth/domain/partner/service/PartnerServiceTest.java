package com.iitsaii.photobooth.domain.partner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.iitsaii.photobooth.domain.partner.entity.Partner;
import com.iitsaii.photobooth.domain.partner.error.PartnerErrorCode;
import com.iitsaii.photobooth.domain.partner.repository.PartnerRepository;
import com.iitsaii.photobooth.global.error.CustomException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartnerServiceTest {

    @Mock
    private PartnerRepository partnerRepository;

    @InjectMocks
    private PartnerService partnerService;

    // 2026-09-08은 화요일이다.
    private static final LocalDateTime TUESDAY_1700 = LocalDateTime.of(2026, 9, 8, 17, 0);

    private Partner partnerOperating(DayOfWeek day, int openHour, int closeHour) {
        return partnerOperating("업체", day, openHour, closeHour);
    }

    private Partner partnerOperating(String name, DayOfWeek day, int openHour, int closeHour) {
        Partner partner = Partner.of(name, "위치", "부제목", "설명", null, null, null, null, "쿠폰",
                null, null, null, null);
        partner.updateOperatingHours(List.of(day), openHour, closeHour);
        return partner;
    }

    @Nested
    @DisplayName("assignRandomPartner")
    class AssignRandomPartner {

        @Test
        @DisplayName("영업 중인 업체도, fallback 대상(피치못한/반짝)도 없으면 NO_ACTIVE_PARTNER 예외를 던진다")
        void throwsWhenNoneOperatingAndNoFallbackCandidate() {
            Partner closedToday = partnerOperating(DayOfWeek.MONDAY, 12, 22);
            given(partnerRepository.findAvailableOrderByLastAssignedSeqAsc()).willReturn(List.of(closedToday));

            assertThatThrownBy(() -> partnerService.assignRandomPartner(TUESDAY_1700))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(PartnerErrorCode.NO_ACTIVE_PARTNER);
        }

        @Test
        @DisplayName("영업 중인 업체가 없으면 피치못한/반짝 중에서만 후보를 고른다")
        void fallsBackToDesignatedPartnersWhenNoneOperating() {
            Partner peachmotan = partnerOperating("피치못한", DayOfWeek.MONDAY, 12, 22);
            Partner banjjak = partnerOperating("반짝", DayOfWeek.MONDAY, 12, 22);
            Partner others = partnerOperating("마주하다", DayOfWeek.MONDAY, 12, 22);
            given(partnerRepository.findAvailableOrderByLastAssignedSeqAsc())
                    .willReturn(List.of(peachmotan, banjjak, others));
            given(partnerRepository.findMaxAssignedSeq()).willReturn(null);

            Partner selected = partnerService.assignRandomPartner(TUESDAY_1700);

            assertThat(selected).isIn(peachmotan, banjjak);
            assertThat(others.getEligibleCount()).isZero();
        }

        @Test
        @DisplayName("영업 중인 업체가 없고 fallback 대상 중 하나만 있으면 그 업체가 배정된다")
        void fallsBackToSingleDesignatedPartnerWhenOnlyOneExists() {
            Partner peachmotan = partnerOperating("피치못한", DayOfWeek.MONDAY, 12, 22);
            given(partnerRepository.findAvailableOrderByLastAssignedSeqAsc()).willReturn(List.of(peachmotan));
            given(partnerRepository.findMaxAssignedSeq()).willReturn(null);

            Partner selected = partnerService.assignRandomPartner(TUESDAY_1700);

            assertThat(selected).isEqualTo(peachmotan);
        }

        @Test
        @DisplayName("영업 중이지 않은 업체는 후보에서 제외되고, 영업 중인 업체만 배정된다")
        void excludesNonOperatingPartners() {
            Partner operating = partnerOperating(DayOfWeek.TUESDAY, 15, 20);
            Partner closed = partnerOperating(DayOfWeek.MONDAY, 12, 22);
            given(partnerRepository.findAvailableOrderByLastAssignedSeqAsc())
                    .willReturn(List.of(operating, closed));
            given(partnerRepository.findMaxAssignedSeq()).willReturn(null);

            Partner selected = partnerService.assignRandomPartner(TUESDAY_1700);

            assertThat(selected).isEqualTo(operating);
            assertThat(closed.getEligibleCount()).isZero();
        }

        @Test
        @DisplayName("영업 중인 후보들의 배정 비율이 같이 오르내려, 비율이 더 낮은 업체가 우선 배정된다")
        void prefersLowerAssignmentRatio() {
            Partner busy = partnerOperating(DayOfWeek.TUESDAY, 15, 20);
            busy.recordEligible();
            busy.recordEligible();
            busy.recordAssigned();
            busy.recordAssigned(); // ratio = 2/2 = 1.0

            Partner idle = partnerOperating(DayOfWeek.TUESDAY, 15, 20);
            idle.recordEligible();
            idle.recordEligible(); // ratio = 0/2 = 0.0

            given(partnerRepository.findAvailableOrderByLastAssignedSeqAsc())
                    .willReturn(List.of(busy, idle));
            given(partnerRepository.findMaxAssignedSeq()).willReturn(2L);

            Partner selected = partnerService.assignRandomPartner(TUESDAY_1700);

            assertThat(selected).isEqualTo(idle);
        }

        @Test
        @DisplayName("eligibleCount를 올리기 전, 기존 누적 비율로 후보를 선정한다 (선증가로 비율 역전 방지)")
        void selectsByRatioBeforeIncrementingEligible() {
            // A: 10/11 = 0.909..., B: 1/1 = 1.0 → 지금은 A가 더 낮다.
            // eligibleCount를 먼저 올려버리면 A=10/12=0.833, B=1/2=0.5로 역전되어 B가 뽑히는 버그가 있었다.
            Partner a = partnerOperating(DayOfWeek.TUESDAY, 15, 20);
            for (int i = 0; i < 11; i++) {
                a.recordEligible();
            }
            for (int i = 0; i < 10; i++) {
                a.recordAssigned();
            }

            Partner b = partnerOperating(DayOfWeek.TUESDAY, 15, 20);
            b.recordEligible();
            b.recordAssigned();

            given(partnerRepository.findAvailableOrderByLastAssignedSeqAsc())
                    .willReturn(List.of(a, b));
            given(partnerRepository.findMaxAssignedSeq()).willReturn(11L);

            Partner selected = partnerService.assignRandomPartner(TUESDAY_1700);

            assertThat(selected).isEqualTo(a);
        }

        @Test
        @DisplayName("영업 중인 모든 후보의 eligibleCount를 1씩 증가시키고, 선정된 업체의 assignedCount만 증가시킨다")
        void incrementsEligibleForAllAndAssignedForSelectedOnly() {
            Partner a = partnerOperating(DayOfWeek.TUESDAY, 15, 20);
            Partner b = partnerOperating(DayOfWeek.TUESDAY, 15, 20);
            given(partnerRepository.findAvailableOrderByLastAssignedSeqAsc()).willReturn(List.of(a, b));
            given(partnerRepository.findMaxAssignedSeq()).willReturn(null);

            Partner selected = partnerService.assignRandomPartner(TUESDAY_1700);
            Partner notSelected = selected == a ? b : a;

            assertThat(selected.getEligibleCount()).isEqualTo(1);
            assertThat(selected.getAssignedCount()).isEqualTo(1);
            assertThat(notSelected.getEligibleCount()).isEqualTo(1);
            assertThat(notSelected.getAssignedCount()).isEqualTo(0);
        }
    }
}
