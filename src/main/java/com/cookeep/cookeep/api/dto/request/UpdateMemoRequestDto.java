package com.cookeep.cookeep.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "메모 변경 요청 DTO")
@Getter
@NoArgsConstructor
public class UpdateMemoRequestDto {

    @Schema(
            description = "수정할 메모 내용 (빈 문자열 허용 = 메모 삭제)",
            example = "샐러드에 사용할 예정",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "메모는 필수입니다.")
    @Size(max = 100, message = "메모는 100자를 초과할 수 없습니다.")
    private String memo;
}
