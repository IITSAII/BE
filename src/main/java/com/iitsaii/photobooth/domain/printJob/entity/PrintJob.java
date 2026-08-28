package com.iitsaii.photobooth.domain.printJob.entity;

import com.iitsaii.photobooth.domain.session.entity.Session;
import com.iitsaii.photobooth.global.entity.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/** 프레임/필터가 합성된 최종 인화용 이미지와 인쇄 상태를 관리한다. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "print_jobs",
        uniqueConstraints = @UniqueConstraint(columnNames = "session_id")
)
public class PrintJob extends BaseEntity {

    /** 연결된 세션 (sessions.id 참조) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    /** 적용된 프레임 종류 (2종 중 선택, 세션당 1개) */
    @Enumerated(EnumType.STRING)
    @Column(name = "frame_type", length = 20, nullable = false)
    private FrameType frameType;

    @Column(name = "filter_bw", nullable = false)
    private boolean filterBw;

    @Column(name = "filter_brightness", nullable = false)
    private Integer filterBrightness;

    /** 프레임/필터 합성 완료된 최종 인화용 이미지 경로 */
    @Column(name = "final_image_url", length = 500)
    private String finalImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrintJobStatus status;

    @Column(name = "printed_at")
    private LocalDateTime printedAt;

    public static PrintJob of(
            Session session,
            FrameType frameType,
            boolean filterBw,
            Integer filterBrightness
    ) {
        PrintJob printJob = new PrintJob();
        printJob.session = session;
        printJob.frameType = frameType;
        printJob.filterBw = filterBw;
        printJob.filterBrightness = filterBrightness;
        printJob.finalImageUrl = null;
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

    public void updateFinalImage(String finalImageUrl) {
        this.finalImageUrl = finalImageUrl;
    }
}
