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
import org.springframework.http.MediaType;
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

    @Operation(
            summary = "최종 인쇄 이미지 업로드",
            description = """
                    프론트가 프레임, 흑백, 밝기를 적용해 생성한 최종 4컷 이미지를 업로드한다.
                    - FRAME 선택 이후 생성된 PrintJob에 최종 이미지 URL을 저장한다.
                    - 업로드가 완료되면 인쇄 에이전트가 사용할 finalImageUrl이 생성된다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업로드 성공"),
            @ApiResponse(responseCode = "400", description = "FRAME 단계가 아니거나 이미지가 비어 있음"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 세션 또는 인쇄 작업")
    })
    @PostMapping(
            value = "/final-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public CommonResponse<PrintJobResDTO.UploadFinalImage> uploadFinalImage(
            @Parameter(description = "세션의 공개 식별자")
            @PathVariable String sessionId,
            @ModelAttribute @Valid PrintJobReqDTO.UploadFinalImage dto
    ) {
        return CommonResponse.ok(printJobService.uploadFinalImage(sessionId, dto.finalImage()));
    }

    @Operation(
            summary = "최종 인쇄 이미지 조회",
            description = """
                    프론트 또는 맥북 프린트 에이전트가 최종 인쇄 정보를 조회한다.
                    - 프레임 선택 및 최종 이미지 업로드가 완료된 세션만 조회할 수 있다.
                    - 최종 4컷 이미지 URL과 프레임/필터 정보를 함께 반환한다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "최종 인쇄 이미지 조회 성공"),
            @ApiResponse(responseCode = "400", description = "최종 인쇄 이미지가 아직 생성되지 않음 (`PrintJobErrorCode.FINAL_IMAGE_NOT_READY`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 세션 또는 인쇄 작업 (`SessionErrorCode.SESSION_NOT_FOUND`, `PrintJobErrorCode.PRINT_JOB_NOT_FOUND`)")
    })
    @GetMapping
    public CommonResponse<PrintJobResDTO.PrintInfo> getPrintInfo(
            @Parameter(description = "세션의 공개 식별자")
            @PathVariable String sessionId
    ) {
        return CommonResponse.ok(printJobService.getPrintInfo(sessionId));
    }

}
