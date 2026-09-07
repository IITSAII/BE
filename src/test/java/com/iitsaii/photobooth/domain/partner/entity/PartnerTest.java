package com.iitsaii.photobooth.domain.partner.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PartnerTest {

    private Partner newPartner() {
        return Partner.of("업체", "위치", "부제목", "설명", null, null, null, null, "쿠폰",
                null, null, null, null);
    }

    @Nested
    @DisplayName("isOperatingAt")
    class IsOperatingAt {

        @Test
        @DisplayName("영업 요일과 시간에 포함되면 true를 반환한다")
        void trueWhenWithinOperatingDaysAndHours() {
            Partner partner = newPartner();
            partner.updateOperatingHours(List.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY), 15, 20);

            LocalDateTime tuesday1730 = LocalDateTime.of(2026, 9, 8, 17, 30);
            assertThat(tuesday1730.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);

            assertThat(partner.isOperatingAt(tuesday1730)).isTrue();
        }

        @Test
        @DisplayName("영업 요일이 아니면 false를 반환한다")
        void falseWhenNotOperatingDay() {
            Partner partner = newPartner();
            partner.updateOperatingHours(List.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY), 15, 20);

            LocalDateTime monday1730 = LocalDateTime.of(2026, 9, 7, 17, 30);
            assertThat(monday1730.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);

            assertThat(partner.isOperatingAt(monday1730)).isFalse();
        }

        @Test
        @DisplayName("영업 요일이어도 영업 시간 밖이면 false를 반환한다")
        void falseWhenOutsideOperatingHours() {
            Partner partner = newPartner();
            partner.updateOperatingHours(List.of(DayOfWeek.TUESDAY), 15, 20);

            LocalDateTime tuesdayBeforeOpen = LocalDateTime.of(2026, 9, 8, 10, 0);
            LocalDateTime tuesdayAtClose = LocalDateTime.of(2026, 9, 8, 20, 0);

            assertThat(partner.isOperatingAt(tuesdayBeforeOpen)).isFalse();
            assertThat(partner.isOperatingAt(tuesdayAtClose)).isFalse();
        }

        @Test
        @DisplayName("운영 정보가 하나라도 없으면 false를 반환한다 (배정 사고 방지)")
        void falseWhenOperatingInfoMissing() {
            Partner partner = newPartner();

            assertThat(partner.isOperatingAt(LocalDateTime.now())).isFalse();
        }
    }

    @Nested
    @DisplayName("assignmentRatio")
    class AssignmentRatio {

        @Test
        @DisplayName("후보에 든 적이 없으면 0을 반환한다")
        void zeroWhenNeverEligible() {
            Partner partner = newPartner();

            assertThat(partner.assignmentRatio()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("배정된 횟수를 후보였던 횟수로 나눈 값을 반환한다")
        void computesRatio() {
            Partner partner = newPartner();
            partner.recordEligible();
            partner.recordEligible();
            partner.recordEligible();
            partner.recordEligible();
            partner.recordAssigned();

            assertThat(partner.assignmentRatio()).isEqualTo(0.25);
        }
    }
}
