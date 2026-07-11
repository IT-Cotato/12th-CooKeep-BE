package com.cookeep.cookeep.domain.user.application;

import com.cookeep.cookeep.api.dto.response.ProfileImageListResponseDto;
import com.cookeep.cookeep.api.dto.response.ProfileImageResponseDto;
import com.cookeep.cookeep.common.util.ImageFolder;
import com.cookeep.cookeep.domain.user.entity.ProfileImages;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileImageService {

    private final UserReader userReader;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    public String resolveUrl(int imageId) {
        ProfileImages image = ProfileImages.fromId(imageId);
        String folder = ImageFolder.PROFILE_IMAGES.getFolderName();
        return String.format("https://%s.s3.%s.amazonaws.com/%s/%s",
                bucket, region, folder, image.getFileName());
    }

    @Transactional(readOnly = true)
    public ProfileImageListResponseDto getProfileImages(Long userId) {
        // 유저 존재 검증 (탈퇴/유효하지 않은 유저 필터링)
        userReader.readById(userId);

        List<ProfileImageResponseDto> images = Arrays.stream(ProfileImages.values())
                .map(p -> new ProfileImageResponseDto(p.getImageId(), resolveUrl(p.getImageId())))
                .toList();

        return new ProfileImageListResponseDto(images);
    }
}
