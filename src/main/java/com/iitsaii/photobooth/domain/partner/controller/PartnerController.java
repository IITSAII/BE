package com.iitsaii.photobooth.domain.partner.controller;

import com.iitsaii.photobooth.domain.partner.dto.PartnerListResponse;
import com.iitsaii.photobooth.domain.partner.entity.Partner;
import com.iitsaii.photobooth.domain.partner.service.PartnerService;
import com.iitsaii.photobooth.global.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/partners")
public class PartnerController {

    private final PartnerService partnerService;

    @Operation(
            summary = "매거진 페이지용 제휴 업체 목록 조회",
            description = "노출 가능한(active) 제휴 업체 전체 목록을 반환한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
    })
    @GetMapping
    public CommonResponse<List<PartnerListResponse>> getPartners() {
        List<Partner> partners = partnerService.getActivePartners();
        return CommonResponse.ok(partners.stream().map(PartnerListResponse::from).toList());
    }
}
