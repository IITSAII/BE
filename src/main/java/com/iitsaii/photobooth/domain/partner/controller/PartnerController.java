package com.iitsaii.photobooth.domain.partner.controller;

import com.iitsaii.photobooth.domain.partner.dto.PartnerListResponse;
import com.iitsaii.photobooth.domain.partner.entity.Partner;
import com.iitsaii.photobooth.domain.partner.service.PartnerService;
import com.iitsaii.photobooth.global.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.ArrayList;
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
            description = "노출 가능한(active) 제휴 업체 전체 목록을 반환한다. 목록 맨 앞에는 잇사이(우리 서비스) 소개 카드가 고정으로 포함된다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
    })
    @GetMapping
    public CommonResponse<List<PartnerListResponse>> getPartners() {
        List<Partner> partners = partnerService.getActivePartners();

        List<PartnerListResponse> responses = new ArrayList<>();
        responses.add(PartnerListResponse.ourStudio());
        partners.stream().map(PartnerListResponse::from).forEach(responses::add);

        return CommonResponse.ok(responses);
    }
}
