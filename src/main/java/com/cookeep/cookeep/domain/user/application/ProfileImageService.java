package com.cookeep.cookeep.domain.user.application;

import com.cookeep.cookeep.common.util.ImageFolder;
import com.cookeep.cookeep.domain.user.entity.ProfileImages;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileImageService {

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
}
