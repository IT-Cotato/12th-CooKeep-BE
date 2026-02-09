package com.cookeep.cookeep.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Schema(
        name = "NotificationRequest",
        description = "푸시 알림 요청 DTO"
)
@Getter
@Builder
public class NotificationRequestDto {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String body;

    private String imageUrl;
}
