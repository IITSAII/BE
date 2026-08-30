package com.iitsaii.photobooth.domain.printJob.converter;

import com.iitsaii.photobooth.domain.printJob.dto.PrintJobResDTO;
import com.iitsaii.photobooth.domain.printJob.entity.PrintJob;

public class PrintJobConverter {

    public static PrintJobResDTO.FrameSelect toFrameSelect(PrintJob printJob) {
        return new PrintJobResDTO.FrameSelect(printJob.getId(), printJob.getFrameType());
    }

    public static PrintJobResDTO.UploadFinalImage toUploadFinalImage(PrintJob printJob) {
        return PrintJobResDTO.UploadFinalImage.builder()
                .finalImageUrl(printJob.getFinalImageUrl())
                .build();
    }

    public static PrintJobResDTO.PrintInfo toPrintInfo(PrintJob printJob) {
        return PrintJobResDTO.PrintInfo.builder()
                .finalImageUrl(printJob.getFinalImageUrl())
                .frameType(printJob.getFrameType())
                .filterBw(printJob.isFilterBw())
                .filterBrightness(printJob.getFilterBrightness())
                .status(printJob.getStatus())
                .build();
    }

    public static PrintJobResDTO.PrintQueue toPrintQueue(PrintJob printJob) {
        return PrintJobResDTO.PrintQueue.builder()
                .sessionId(printJob.getSession().getSessionId())
                .finalImageUrl(printJob.getFinalImageUrl())
                .frameType(printJob.getFrameType())
                .filterBw(printJob.isFilterBw())
                .filterBrightness(printJob.getFilterBrightness())
                .build();
    }
}
