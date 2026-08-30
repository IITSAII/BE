package com.iitsaii.photobooth.domain.session.controller;

import com.iitsaii.photobooth.global.common.CommonResponse;
import com.iitsaii.photobooth.domain.partner.dto.PartnerResponse;
import com.iitsaii.photobooth.domain.session.dto.SessionCreateRequest;
import com.iitsaii.photobooth.domain.session.dto.SessionCreateResponse;
import com.iitsaii.photobooth.domain.session.dto.SessionRelationshipRequest;
import com.iitsaii.photobooth.domain.session.dto.SessionStatusResponse;
import com.iitsaii.photobooth.domain.session.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    @Operation(
            summary = "세션 생성",
            description = """
                    1단계(수량 선택) 완료 직후 호출한다. 서버가 수량 기반으로 결제 금액을 계산해 확정하고
                    새 세션을 생성한다.
                    - 허용되는 수량은 2, 4, 6뿐이다 (단가: 3,000원 / 6,000원 / 9,000원).
                    - `amount`는 클라이언트가 보내는 값이 아니라 서버가 계산해 확정한 값이다.
                    - 생성된 `sessionId`는 이후 상태 조회, 결제 승인(`orderId`)에서 그대로 재사용된다.
                    - 생성 직후 `currentStep`은 항상 `PAYMENT`다 (수량 선택은 이미 끝난 상태이므로).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "세션 생성 성공"),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 촬영 수량이거나 quantity 누락 " +
                    "(`SessionErrorCode.INVALID_QUANTITY` / `GlobalErrorCode.INVALID_INPUT_VALUE`)"),
    })
    @PostMapping
    public CommonResponse<SessionCreateResponse> createSession(@Valid @RequestBody SessionCreateRequest request) {
        return CommonResponse.ok(sessionService.createSession(request.quantity()));
    }

    @Operation(
            summary = "세션 상태 조회",
            description = """
                    프론트가 결제 완료 여부, 현재 단계, 타임아웃 시각을 확인하기 위해 주기적으로 폴링한다.
                    - `status`는 세션 전체 진행 상태, `currentStep`은 세부 단계를 나타낸다.
                    - `stepExpiresAt`을 지나면 서버가 기본값으로 자동 진행시키므로,
                      프론트는 이 시각 이후 다시 조회해 다음 단계로 넘어갔는지 확인해야 한다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 세션 (`SessionErrorCode.SESSION_NOT_FOUND`)"),
    })
    @GetMapping("/{sessionId}/status")
    public CommonResponse<SessionStatusResponse> getStatus(
            @Parameter(description = "조회할 세션의 공개 식별자", example = "sess_18f65b95c4fb")
            @PathVariable String sessionId
    ) {
        return CommonResponse.ok(sessionService.getStatus(sessionId));
    }

    @Operation(
            summary = "관계 선택",
            description = """
                    2단계(오늘의 관계 선택) 완료 시 호출한다. 결제 완료 후 진입하는 RELATIONSHIP 단계에서만
                    호출할 수 있다.
                    - `relationshipType`을 생략하거나 null로 보내면 "설정 안 함"으로 저장된다.
                    - 성공 시 CAPTURE 단계로 전이되고, 촬영 타임아웃이 새로 설정된다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "관계 선택 성공"),
            @ApiResponse(responseCode = "400", description = "RELATIONSHIP 단계가 아닐 때 호출 " +
                    "(`SessionErrorCode.INVALID_STEP`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 세션 (`SessionErrorCode.SESSION_NOT_FOUND`)"),
    })
    @PostMapping("/{sessionId}/relationship")
    public CommonResponse<SessionStatusResponse> chooseRelationship(
            @Parameter(description = "세션의 공개 식별자", example = "sess_18f65b95c4fb")
            @PathVariable String sessionId,
            @RequestBody SessionRelationshipRequest request
    ) {
        return CommonResponse.ok(sessionService.chooseRelationship(sessionId, request.relationshipType()));
    }

    @Operation(
            summary = "배정된 제휴 업체 조회",
            description = """
                    결제 확정 시 랜덤 배정된 제휴 업체 정보를 조회한다.
                    - 결제가 확정되기 전(PAYMENT 단계)에는 아직 배정된 업체가 없어 404가 반환된다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 세션이거나(`SessionErrorCode.SESSION_NOT_FOUND`) "
                    + "아직 업체가 배정되지 않음(`SessionErrorCode.PARTNER_NOT_ASSIGNED`)"),
    })
    @GetMapping("/{sessionId}/partner")
    public CommonResponse<PartnerResponse> getAssignedPartner(
            @Parameter(description = "세션의 공개 식별자", example = "sess_18f65b95c4fb")
            @PathVariable String sessionId
    ) {
        return CommonResponse.ok(sessionService.getAssignedPartner(sessionId));
    }
}
