package com.iitsaii.photobooth.domain.printJob.controller;

import com.iitsaii.photobooth.domain.printJob.dto.PrintJobResDTO;
import com.iitsaii.photobooth.domain.printJob.service.PrintJobService;
import com.iitsaii.photobooth.global.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인화물(종이 출력물)에 인쇄된 QR/바코드로 접근하는 갤러리 조회 API.
 * sessionId 기반 조회(PrintJobController.getPrintInfo)와 달리 만료 없이 항상 접근 가능하다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gallery/{galleryToken}")
public class GalleryController {

    private final PrintJobService printJobService;

    @Operation(
            summary = "인화물 QR/바코드로 최종 인쇄 이미지 조회",
            description = """
                    인화물에 인쇄된 QR/바코드에 담긴 galleryToken으로 최종 인쇄 이미지를 조회한다.
                    - sessionId 기반 조회(GET /api/sessions/{sessionId}/print)와 달리 열람 기한 없이 항상 조회 가능하다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "최종 인쇄 이미지 조회 성공"),
            @ApiResponse(responseCode = "400", description = "최종 인쇄 이미지가 아직 생성되지 않음 (`PrintJobErrorCode.FINAL_IMAGE_NOT_READY`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 galleryToken이거나 인쇄 작업이 없음 (`SessionErrorCode.SESSION_NOT_FOUND`, `PrintJobErrorCode.PRINT_JOB_NOT_FOUND`)")
    })
    @GetMapping("/print")
    public CommonResponse<PrintJobResDTO.PrintInfo> getPrintInfo(
            @Parameter(description = "인화물 QR/바코드에 담긴 갤러리 토큰")
            @PathVariable UUID galleryToken
    ) {
        return CommonResponse.ok(printJobService.getPrintInfoByGalleryToken(galleryToken));
    }
}
