package com.iitsaii.photobooth.domain.photo.controller;

import com.iitsaii.photobooth.domain.photo.dto.PhotoReqDTO;
import com.iitsaii.photobooth.domain.photo.dto.PhotoResDTO;
import com.iitsaii.photobooth.domain.photo.service.PhotoService;
import com.iitsaii.photobooth.global.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sessions/{sessionId}")
public class PhotoController {

    private final PhotoService photoService;

    @Operation(
            summary = "촬영 사진 업로드",
            description = """
                    CAPTURE 단계에서 촬영한 사진을 1장씩 업로드한다.
                    - CAPTURE 단계에서만 호출할 수 있다.
                    - `shotNumber`는 몇 번째 컷인지 나타내며 1부터 시작한다.
                    - 업로드된 사진은 S3에 저장되고 세션에 연결된다.
                    - 모든 사진이 업로드되면 서버가 다음 단계(SELECT_FRAME)로 진행한다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사진 업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 shotNumber 또는 CAPTURE 단계가 아님"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 세션 (`SessionErrorCode.SESSION_NOT_FOUND`)")
    })
    @PostMapping(value = "/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<PhotoResDTO.SavePhoto> savePhoto(
            @Parameter(description = "세션의 공개 식별자")
            @PathVariable String sessionId,
            @Parameter(description = "촬영 컷 번호 (1부터 시작)", example = "1")
            @RequestParam Integer shotNumber,
            @RequestPart MultipartFile image
    ) {
        return CommonResponse.ok(photoService.savePhoto(sessionId, shotNumber, image));
    }

    @Operation(
            summary = "촬영 사진 목록 조회",
            description = """
                    현재 세션에 업로드된 모든 촬영 사진을 조회한다.
                    - 촬영 순서대로 사진 목록을 반환한다.
                    - 프레임 선택 단계에서 사진 미리보기를 위해 사용한다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사진 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 세션 (`SessionErrorCode.SESSION_NOT_FOUND`)")
    })
    @GetMapping("/photos")
    public CommonResponse<PhotoResDTO.PhotoList> getPhotos(
            @Parameter(description = "세션의 공개 식별자")
            @PathVariable String sessionId
    ) {
        return CommonResponse.ok(photoService.getPhotos(sessionId));
    }

    @Operation(
            summary = "사진 선택 완료",
            description = """
                    사진 4장 선택 완료 시 선택한 사진 정보를 저장한다.
                    - SELECT_PHOTO 단계에서만 호출할 수 있다.
                    - 선택한 사진 번호를 저장하고 다음 단계(FRAME)로 진행한다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사진 선택 성공"),
            @ApiResponse(responseCode = "400", description = "SELECT_PHOTO 단계가 아님 또는 잘못된 선택"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 세션 (`SessionErrorCode.SESSION_NOT_FOUND`)")
    })
    @PostMapping("/photos/select")
    public CommonResponse<Void> selectPhotos(
            @Parameter(description = "세션의 공개 식별자")
            @PathVariable String sessionId,
            @Valid @RequestBody PhotoReqDTO.SelectPhotos dto
    ) {
        photoService.selectPhotos(sessionId, dto);
        return CommonResponse.ok();
    }

    @Operation(
            summary = "선택된 사진 목록 조회",
            description = """
                    사진 선택 단계에서 확정된 사진 4장을 조회한다.
                    - 사진은 사용자가 선택한 순서(select_order)대로 반환된다.
                    - 프레임 선택 화면, 인쇄 미리보기 화면 등에서 사용한다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "선택된 사진 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 세션 (`SessionErrorCode.SESSION_NOT_FOUND`)")
    })
    @GetMapping("/selected-photos")
    public CommonResponse<PhotoResDTO.SelectedPhotoList> getSelectedPhotos(
            @Parameter(description = "세션의 공개 식별자")
            @PathVariable String sessionId
    ) {
        return CommonResponse.ok(photoService.getSelectedPhotos(sessionId));
    }
}
