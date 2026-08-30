package com.cookeep.cookeep.domain.user.entity;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProfileImages {

    IMAGE_1(1, "1_v2.png"),
    IMAGE_2(2, "2_v2.png"),
    IMAGE_3(3, "3_v2.png"),
    IMAGE_4(4, "4_v2.png"),
    IMAGE_5(5, "5_v2.png"),
    IMAGE_6(6, "6_v2.png"),
    IMAGE_7(7, "7_v2.png"),
    IMAGE_8(8, "8_v2.png"),
    IMAGE_9(9, "9_v2.png"),
    IMAGE_10(10, "10_v2.png"),
    IMAGE_11(11, "11_v2.png"),
    IMAGE_12(12, "12_v2.png");

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
