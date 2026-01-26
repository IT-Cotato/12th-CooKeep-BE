package com.cookeep.cookeep.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(name = "NicknameUpdateRequest", description = "닉네임 수정 요청 DTO")
public class NicknameUpdateRequestDto {

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(max = 10, message = "닉네임은 1자 이상 10자 이하여야 합니다.")
    @Schema(description = "변경할 닉네임", example = "새로운닉네임", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;
}
