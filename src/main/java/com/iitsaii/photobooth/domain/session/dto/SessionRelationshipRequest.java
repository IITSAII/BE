package com.iitsaii.photobooth.domain.session.dto;

import com.iitsaii.photobooth.domain.session.entity.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관계 선택 요청")
public record SessionRelationshipRequest(

        @Schema(description = """
                오늘의 관계 (선택). 생략하거나 null이면 "설정 안 함"으로 처리되어 저장된다.
                """, example = "COUPLE", nullable = true)
        RelationshipType relationshipType
) {
}
