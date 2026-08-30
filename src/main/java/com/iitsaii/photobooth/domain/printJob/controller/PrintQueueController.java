package com.iitsaii.photobooth.domain.printJob.controller;

import com.iitsaii.photobooth.domain.printJob.dto.PrintJobResDTO;
import com.iitsaii.photobooth.domain.printJob.service.PrintJobService;
import com.iitsaii.photobooth.global.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PrintQueueController {

    private final PrintJobService printJobService;

    @Operation(
            summary = "인쇄 대기 작업 조회",
            description = """
                맥북 프린트 에이전트가 출력 대기 중인 인쇄 작업을 조회한다.
                - 가장 먼저 생성된 QUEUED 상태의 인쇄 작업 1건을 반환한다.
                - 반환된 최종 이미지 URL을 사용해 프린터에서 인쇄를 진행한다.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인쇄 대기 작업 조회 성공"),
            @ApiResponse(responseCode = "404", description = "인쇄 대기 작업 없음")
    })
    @GetMapping("/api/print/queue")
    public CommonResponse<PrintJobResDTO.PrintQueue> getPrintQueue() {
        return CommonResponse.ok(printJobService.getPrintQueue());
    }
}
