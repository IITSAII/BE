package com.iitsaii.photobooth.domain.printJob.controller;

import com.iitsaii.photobooth.domain.printJob.dto.PrintJobReqDTO;
import com.iitsaii.photobooth.domain.printJob.dto.PrintJobResDTO;
import com.iitsaii.photobooth.domain.printJob.service.PrintJobService;
import com.iitsaii.photobooth.global.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sessions/{sessionId}/print")
public class PrintJobController {

    private final PrintJobService printJobService;

    @Operation(
            summary = "인쇄 시작",
            description = """
                    프레임 선택 화면에서 '다음' 버튼을 누르면 호출한다.
                    - 프레임, 흑백 여부, 밝기를 저장한다.
                    - 최종 인쇄 이미지를 생성한다.
                    - PrintJob을 생성하고 PRINT 단계로 이동한다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인쇄 시작 성공"),
            @ApiResponse(responseCode = "400", description = "FRAME 단계가 아니거나 잘못된 밝기 값"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 세션 (`SessionErrorCode.SESSION_NOT_FOUND`)")
    })
    @PostMapping
    public CommonResponse<PrintJobResDTO.FrameSelect> selectFrame(
            @Parameter(description = "세션의 공개 식별자")
            @PathVariable String sessionId,
            @Valid @RequestBody PrintJobReqDTO.FrameSelect dto
    ) {
        return CommonResponse.ok(printJobService.selectFrame(sessionId, dto));
    }

}
