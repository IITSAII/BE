package com.iitsaii.photobooth.domain.printJob.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iitsaii.photobooth.domain.printJob.entity.FilterType;
import com.iitsaii.photobooth.domain.printJob.entity.FrameType;
import com.iitsaii.photobooth.domain.printJob.entity.PrintJob;
import com.iitsaii.photobooth.domain.session.entity.Session;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PrintJobConverterTest {

    @Test
    @DisplayName("큐 응답에는 프린트 에이전트가 아는 필드만 담는다")
    void printQueueContainsOnlyFieldsKnownToAgent() {
        // 에이전트는 모르는 필드가 오면 응답을 읽지 못해 인쇄가 멈춘다(작업은 이미 PRINTING으로 바뀐 뒤다).
        // 필드를 추가하려면 에이전트가 먼저 그 필드를 받을 수 있게 배포돼야 한다.
        PrintJob printJob = PrintJob.of(Session.of("sess_test", 2, 3000), FrameType.IVORY, FilterType.VINTAGE, 0);

        JsonNode json = new ObjectMapper().valueToTree(PrintJobConverter.toPrintQueue(printJob));
        List<String> fields = new ArrayList<>();
        json.fieldNames().forEachRemaining(fields::add);

        assertThat(fields).containsExactlyInAnyOrder(
                "sessionId", "finalImageUrl", "frameType", "filterBw", "filterBrightness", "quantity");
    }
}
