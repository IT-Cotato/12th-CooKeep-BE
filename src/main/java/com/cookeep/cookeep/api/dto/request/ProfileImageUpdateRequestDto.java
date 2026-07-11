package com.cookeep.cookeep.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record ProfileImageUpdateRequestDto(

        @NotNull
        Integer imageId
) { }
