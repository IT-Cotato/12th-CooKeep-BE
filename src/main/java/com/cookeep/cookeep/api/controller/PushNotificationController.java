package com.cookeep.cookeep.api.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "푸시 알림", description = "유통기한 임박 식재료 팝업 알림 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alerts")
public class PushNotificationController {


}
