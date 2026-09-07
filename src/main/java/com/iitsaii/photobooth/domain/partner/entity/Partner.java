package com.iitsaii.photobooth.domain.partner.entity;

import com.iitsaii.photobooth.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 제휴 업체 정보 및 쿠폰. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "partners")
public class Partner extends BaseEntity {

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 200)
    private String location;

    @Column(name = "short_description", length = 200)
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 매장 로고 이미지 */
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    /** 매장 대표 사진 */
    @Column(name = "thumbnail_image_url", length = 500)
    private String thumbnailImageUrl;

    /** 매장 사진 */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** 네이버 지도 길찾기 URL */
    @Column(name = "direction_url", length = 500)
    private String directionUrl;

    /** 실제 쿠폰 혜택 내용 (예: 아메리카노 1잔 무료) */
    @Column(name = "coupon_description", length = 200)
    private String couponDescription;

    /** 배경 이미지 (파트너 상세 화면 상단) */
    @Column(name = "background_image_url", length = 500)
    private String backgroundImageUrl;

    /** 프로필 이미지 (파트너 상세 화면에서 매장 로고와 별도로 노출) */
    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    /** 협약에 참여한 담당자/참가자 이름 목록 */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "participant_names", columnDefinition = "text[]")
    private List<String> participantNames;

    /** 영업 시간 (예: "Mon – Thu. PM 14:00 ~ 24:00") */
    @Column(name = "business_hours", length = 200)
    private String businessHours;

    /** 업체 증감 시 삭제 대신 비활성화 처리하는 플래그 */
    @Column(name = "is_active", nullable = false)
    private boolean active;

    /** 협약 만료일 (내부 관리용, 폐업/재계약 여부 파악에 사용. 값이 없으면 만료일 미정) */
    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    /**
     * 최근 당첨 순번(전역 시퀀스). 랜덤 배정 시 이 값이 가장 큰(=가장 최근 당첨된) 업체 1곳만 후보에서 제외하는 데 사용.
     * 특정 업체가 연속으로 당첨되는 상황을 최소화하는 게 목적이다.
     */
    @Column(name = "last_assigned_seq")
    private Long lastAssignedSeq;

    /** 영업 요일 목록 (예: [MONDAY, TUESDAY, WEDNESDAY, THURSDAY]). 값이 없으면 배정 후보에서 제외된다. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "operating_days", columnDefinition = "text[]")
    private List<DayOfWeek> operatingDays;

    /** 영업 시작 시각 (0~23시) */
    @Column(name = "open_hour")
    private Integer openHour;

    /** 영업 종료 시각 (1~24시, 24는 자정을 의미) */
    @Column(name = "close_hour")
    private Integer closeHour;

    /**
     * 누적 배정 횟수. eligibleCount와 함께 배정 비율(assignedCount / eligibleCount)을 계산해
     * 영업일 수가 다른 업체끼리도 공평하게 비교하는 데 사용한다. 리셋 없이 계속 누적된다.
     */
    @Column(name = "assigned_count", nullable = false)
    private int assignedCount;

    /**
     * 영업 중이어서 배정 후보에 포함됐던 누적 횟수. 실제 당첨 여부와 무관하게, 후보 풀에 들 때마다 증가한다.
     */
    @Column(name = "eligible_count", nullable = false)
    private int eligibleCount;

    public static Partner of(
            String name,
            String location,
            String shortDescription,
            String description,
            String logoUrl,
            String thumbnailImageUrl,
            String imageUrl,
            String directionUrl,
            String couponDescription,
            String backgroundImageUrl,
            String profileImageUrl,
            List<String> participantNames,
            String businessHours
    ) {
        Partner partner = new Partner();
        partner.name = name;
        partner.location = location;
        partner.shortDescription = shortDescription;
        partner.description = description;
        partner.logoUrl = logoUrl;
        partner.thumbnailImageUrl = thumbnailImageUrl;
        partner.imageUrl = imageUrl;
        partner.backgroundImageUrl = backgroundImageUrl;
        partner.profileImageUrl = profileImageUrl;
        partner.participantNames = participantNames;
        partner.businessHours = businessHours;
        partner.directionUrl = directionUrl;
        partner.couponDescription = couponDescription;
        partner.active = true;
        return partner;
    }

    /** 전역 시퀀스에서 발급받은 다음 순번을 최근 당첨 순번으로 기록한다. */
    public void assignNow(long nextSeq) {
        this.lastAssignedSeq = nextSeq;
    }

    public void updateContractEndDate(LocalDate contractEndDate) {
        this.contractEndDate = contractEndDate;
    }

    public void updateOperatingHours(List<DayOfWeek> operatingDays, Integer openHour, Integer closeHour) {
        this.operatingDays = operatingDays;
        this.openHour = openHour;
        this.closeHour = closeHour;
    }

    public void deactivate() {
        this.active = false;
    }

    /**
     * now 시점에 영업 중인지 판단한다. operatingDays/openHour/closeHour 중 하나라도 설정되지 않았으면
     * 배정 후보에서 제외하기 위해 false를 반환한다 (운영정보 미입력 업체가 배정되는 사고 방지).
     * 시각 비교는 시간 단위로만 하며(분 단위 미지원), closeHour=24는 자정을 의미한다.
     */
    public boolean isOperatingAt(LocalDateTime now) {
        if (operatingDays == null || operatingDays.isEmpty() || openHour == null || closeHour == null) {
            return false;
        }
        if (!operatingDays.contains(now.getDayOfWeek())) {
            return false;
        }
        int hour = now.getHour();
        return hour >= openHour && hour < closeHour;
    }

    /** 영업 중이어서 배정 후보에 포함됐음을 기록한다. */
    public void recordEligible() {
        this.eligibleCount++;
    }

    /** 실제로 당첨되어 배정됐음을 기록한다. */
    public void recordAssigned() {
        this.assignedCount++;
    }

    /**
     * 영업 기회 대비 배정된 비율. 영업일 수가 다른 업체끼리도 이 값으로 비교하면
     * 공평하게 배정 우선순위를 매길 수 있다. 후보에 든 적이 없으면(eligibleCount=0) 0으로 취급해
     * 아직 기회를 못 받은 업체가 우선 후보가 되도록 한다.
     */
    public double assignmentRatio() {
        return eligibleCount == 0 ? 0.0 : (double) assignedCount / eligibleCount;
    }
}
