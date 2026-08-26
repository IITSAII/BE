package com.iitsaii.photobooth.printjob.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/** 프레임/필터가 합성된 최종 인화용 이미지와 인쇄 상태를 관리한다. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "print_jobs")
public class PrintJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 연결된 세션 (sessions.id 참조) */
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /** 적용된 프레임 종류 (2종 중 선택, 세션당 1개) */
    @Column(name = "frame_type", length = 20, nullable = false)
    private String frameType;

    @Column(name = "filter_bw", nullable = false)
    private boolean filterBw;

    @Column(name = "filter_brightness", nullable = false)
    private Integer filterBrightness;

    /** 프레임/필터 합성 완료된 최종 인화용 이미지 경로 */
    @Column(name = "final_image_url", length = 500, nullable = false)
    private String finalImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrintJobStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "printed_at")
    private LocalDateTime printedAt;

    public static PrintJob of(
            Long sessionId,
            String frameType,
            boolean filterBw,
            Integer filterBrightness,
            String finalImageUrl
    ) {
        PrintJob printJob = new PrintJob();
        printJob.sessionId = sessionId;
        printJob.frameType = frameType;
        printJob.filterBw = filterBw;
        printJob.filterBrightness = filterBrightness;
        printJob.finalImageUrl = finalImageUrl;
        printJob.status = PrintJobStatus.QUEUED;
        return printJob;
    }

    public void markPrinting() {
        this.status = PrintJobStatus.PRINTING;
    }

    public void markDone() {
        this.status = PrintJobStatus.DONE;
        this.printedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = PrintJobStatus.FAILED;
    }
}
