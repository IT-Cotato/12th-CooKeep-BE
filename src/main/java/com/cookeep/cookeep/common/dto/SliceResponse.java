package com.cookeep.cookeep.common.dto;

import org.springframework.data.domain.Slice;

import java.util.List;

public record SliceResponse<T>(
        List<T> content,
        boolean last,
        int number,
        int size,
        int numberOfElements
) {
    public static <T> SliceResponse<T> from(Slice<T> slice) {
        return new SliceResponse<>(
                slice.getContent(),
                slice.isLast(),
                slice.getNumber(),
                slice.getSize(),
                slice.getNumberOfElements()
        );
    }
}
