package com.iitsaii.photobooth.domain.printJob.converter;

import com.iitsaii.photobooth.domain.printJob.dto.PrintJobResDTO;
import com.iitsaii.photobooth.domain.printJob.entity.PrintJob;

public class PrintJobConverter {

    public static PrintJobResDTO.FrameSelect toFrameSelect(PrintJob printJob) {
        return new PrintJobResDTO.FrameSelect(printJob.getId(), printJob.getFrameType());
    }
}
