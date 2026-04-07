package com.cookeep.cookeep.config;

import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.Security;

@Configuration
public class WebPushConfig {

    @Value("${vapid.public-key}")
    private String vapidPublicKey;

    @Value("${vapid.private-key}")
    private String vapidPrivateKey;

    @Bean
    public PushService pushService() throws Exception {
        // BouncyCastle Provider 등록 (EC 키 처리에 필요)
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        return new PushService(vapidPublicKey, vapidPrivateKey);
    }
}
