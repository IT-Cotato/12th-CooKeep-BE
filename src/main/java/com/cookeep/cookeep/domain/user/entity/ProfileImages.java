package com.cookeep.cookeep.domain.user.entity;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProfileImages {

    IMAGE_1(1, "1.png"),
    IMAGE_2(2, "2.png"),
    IMAGE_3(3, "3.png"),
    IMAGE_4(4, "4.png"),
    IMAGE_5(5, "5.png"),
    IMAGE_6(6, "6.png");

    private final int imageId;
    private final String fileName;

    public static ProfileImages fromId(int imageId) {
        for (ProfileImages image : ProfileImages.values()) {
            if (image.imageId == imageId) {
                return image;
            }
        }
        throw new AppException(ErrorCode.INVALID_PROFILE_IMAGE_ID);
    }
}
