package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.recipe.entity.AiRecipe;
import com.cookeep.cookeep.domain.recipe.entity.AiSession;
import com.cookeep.cookeep.domain.recipe.entity.Feature;
import com.cookeep.cookeep.domain.user.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

class CookeepsRecipeDetailResponseDtoTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 1, 12, 30, 0);
    private static final String CONTENT_JSON = "{\"steps\":[\"끓인다\"]}";

    private User author;

    @BeforeEach
    void setUp() {
        author = User.builder().userId(10L).nickname("작성자").build();
    }

    private DailyRecipe.DailyRecipeBuilder baseRecipe() {
        return DailyRecipe.builder()
                .id(1L)
                .title("된장찌개")
                .description("집밥 느낌 물씬")
                .content(CONTENT_JSON)
                .recipeImageUrl("https://example.com/img.jpg")
                .croppedImageUrl("https://example.com/cropped.jpg")
                .isPublic(true)
                .likeCount(15)
                .user(author);
    }

    private DailyRecipe withCreatedAt(DailyRecipe recipe) {
        ReflectionTestUtils.setField(recipe, "createdAt", CREATED_AT);
        return recipe;
    }

    private DailyRecipe recipeWithFeature(Feature feature) {
        AiSession session = AiSession.builder().feature(feature).build();
        AiRecipe aiRecipe = AiRecipe.builder().session(session).build();
        return withCreatedAt(baseRecipe().aiRecipe(aiRecipe).build());
    }

    @Nested
    @DisplayName("from - 기존 필드 매핑")
    class FromExistingFields {

        @Test
        @DisplayName("DailyRecipe의 기본 필드가 DTO에 그대로 매핑된다")
        void 기본_필드_매핑() {
            DailyRecipe recipe = recipeWithFeature(Feature.RICE_BOWL);

            CookeepsRecipeDetailResponseDto dto = CookeepsRecipeDetailResponseDto.from(recipe, false, false, false);

            assertThat(dto.getDailyRecipeId()).isEqualTo(1L);
            assertThat(dto.getNickname()).isEqualTo("작성자");
            assertThat(dto.getTitle()).isEqualTo("된장찌개");
            assertThat(dto.getDescription()).isEqualTo("집밥 느낌 물씬");
            assertThat(dto.getContent()).isEqualTo(CONTENT_JSON);
            assertThat(dto.getRecipeImageUrl()).isEqualTo("https://example.com/img.jpg");
            assertThat(dto.getCroppedImageUrl()).isEqualTo("https://example.com/cropped.jpg");
            assertThat(dto.getLikeCount()).isEqualTo(15);
            assertThat(dto.getCreatedAt()).isEqualTo(CREATED_AT);
        }

        @Test
        @DisplayName("feature가 있으면 feature와 한글 표시명(featureName)이 매핑된다")
        void feature_매핑() {
            DailyRecipe recipe = recipeWithFeature(Feature.RICE_BOWL);

            CookeepsRecipeDetailResponseDto dto = CookeepsRecipeDetailResponseDto.from(recipe, false, false, false);

            assertThat(dto.getFeature()).isEqualTo(Feature.RICE_BOWL);
            assertThat(dto.getFeatureName()).isEqualTo("밥/덮밥");
        }

        @Test
        @DisplayName("aiRecipe가 없는 레시피는 feature와 featureName이 null이다")
        void aiRecipe_없으면_feature_null() {
            DailyRecipe recipe = withCreatedAt(baseRecipe().aiRecipe(null).build());

            CookeepsRecipeDetailResponseDto dto = CookeepsRecipeDetailResponseDto.from(recipe, false, false, false);

            assertThat(dto.getFeature()).isNull();
            assertThat(dto.getFeatureName()).isNull();
        }

        @Test
        @DisplayName("aiRecipe는 있지만 session이 없으면 feature와 featureName이 null이다")
        void session_없으면_feature_null() {
            AiRecipe aiRecipe = AiRecipe.builder().session(null).build();
            DailyRecipe recipe = withCreatedAt(baseRecipe().aiRecipe(aiRecipe).build());

            CookeepsRecipeDetailResponseDto dto = CookeepsRecipeDetailResponseDto.from(recipe, false, false, false);

            assertThat(dto.getFeature()).isNull();
            assertThat(dto.getFeatureName()).isNull();
        }

        @Test
        @DisplayName("한줄평과 이미지 URL이 없는 레시피는 해당 필드가 null로 매핑된다")
        void 선택_필드_null_매핑() {
            DailyRecipe recipe = withCreatedAt(baseRecipe()
                    .description(null)
                    .recipeImageUrl(null)
                    .croppedImageUrl(null)
                    .build());

            CookeepsRecipeDetailResponseDto dto = CookeepsRecipeDetailResponseDto.from(recipe, false, false, false);

            assertThat(dto.getDescription()).isNull();
            assertThat(dto.getRecipeImageUrl()).isNull();
            assertThat(dto.getCroppedImageUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("from - viewer 상태 필드 매핑")
    class FromViewerState {

        @Test
        @DisplayName("타인 레시피이면 canLike, canBookmark가 true이고 isLiked, isBookmarked는 전달값 그대로다")
        void 타인_레시피() {
            DailyRecipe recipe = withCreatedAt(baseRecipe().build());

            CookeepsRecipeDetailResponseDto dto = CookeepsRecipeDetailResponseDto.from(recipe, true, false, false);

            assertThat(dto.getIsLiked()).isTrue();
            assertThat(dto.getIsBookmarked()).isFalse();
            assertThat(dto.getCanLike()).isTrue();
            assertThat(dto.getCanBookmark()).isTrue();
        }

        @Test
        @DisplayName("이미 좋아요/북마크한 타인 레시피도 canLike, canBookmark는 true이다 (상태와 권한은 독립)")
        void 이미_좋아요_북마크한_타인_레시피() {
            DailyRecipe recipe = withCreatedAt(baseRecipe().build());

            CookeepsRecipeDetailResponseDto dto = CookeepsRecipeDetailResponseDto.from(recipe, true, true, false);

            assertThat(dto.getIsLiked()).isTrue();
            assertThat(dto.getIsBookmarked()).isTrue();
            assertThat(dto.getCanLike()).isTrue();
            assertThat(dto.getCanBookmark()).isTrue();
        }

        @Test
        @DisplayName("본인 레시피이면 canLike, canBookmark가 false이다")
        void 본인_레시피() {
            DailyRecipe recipe = withCreatedAt(baseRecipe().build());

            CookeepsRecipeDetailResponseDto dto = CookeepsRecipeDetailResponseDto.from(recipe, false, false, true);

            assertThat(dto.getIsLiked()).isFalse();
            assertThat(dto.getIsBookmarked()).isFalse();
            assertThat(dto.getCanLike()).isFalse();
            assertThat(dto.getCanBookmark()).isFalse();
        }
    }

    @Nested
    @DisplayName("JSON 직렬화")
    class Serialization {

        private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        private JsonNode serialize(CookeepsRecipeDetailResponseDto dto) throws Exception {
            return objectMapper.readTree(objectMapper.writeValueAsString(dto));
        }

        @Test
        @DisplayName("응답 JSON의 키 집합이 기존 필드와 viewer 상태 필드를 모두 포함하며 그 외 키는 없다")
        void 응답_키_집합() throws Exception {
            DailyRecipe recipe = recipeWithFeature(Feature.RICE_BOWL);

            JsonNode json = serialize(CookeepsRecipeDetailResponseDto.from(recipe, true, false, false));

            Set<String> keys = new TreeSet<>();
            json.fieldNames().forEachRemaining(keys::add);
            assertThat(keys).containsExactlyInAnyOrder(
                    "dailyRecipeId", "nickname", "title", "description", "content",
                    "recipeImageUrl", "croppedImageUrl", "likeCount", "feature", "featureName", "createdAt",
                    "isLiked", "isBookmarked", "canLike", "canBookmark");
        }

        @Test
        @DisplayName("기존 필드의 값이 JSON에 올바르게 직렬화된다")
        void 기존_필드_값() throws Exception {
            DailyRecipe recipe = recipeWithFeature(Feature.RICE_BOWL);

            JsonNode json = serialize(CookeepsRecipeDetailResponseDto.from(recipe, false, false, false));

            assertThat(json.get("dailyRecipeId").asLong()).isEqualTo(1L);
            assertThat(json.get("nickname").asText()).isEqualTo("작성자");
            assertThat(json.get("title").asText()).isEqualTo("된장찌개");
            assertThat(json.get("description").asText()).isEqualTo("집밥 느낌 물씬");
            assertThat(json.get("recipeImageUrl").asText()).isEqualTo("https://example.com/img.jpg");
            assertThat(json.get("croppedImageUrl").asText()).isEqualTo("https://example.com/cropped.jpg");
            assertThat(json.get("likeCount").asInt()).isEqualTo(15);
            assertThat(json.get("feature").asText()).isEqualTo("RICE_BOWL");
            assertThat(json.get("featureName").asText()).isEqualTo("밥/덮밥");
            assertThat(json.hasNonNull("createdAt")).isTrue();
        }

        @Test
        @DisplayName("content는 문자열이 아니라 JSON 객체 그대로 내려간다 (@JsonRawValue)")
        void content_raw_JSON() throws Exception {
            DailyRecipe recipe = recipeWithFeature(Feature.RICE_BOWL);

            JsonNode json = serialize(CookeepsRecipeDetailResponseDto.from(recipe, false, false, false));

            assertThat(json.get("content").isObject()).isTrue();
            assertThat(json.get("content").get("steps").get(0).asText()).isEqualTo("끓인다");
        }

        @Test
        @DisplayName("feature가 없으면 feature와 featureName은 null로 직렬화된다")
        void feature_없으면_null_직렬화() throws Exception {
            DailyRecipe recipe = withCreatedAt(baseRecipe().aiRecipe(null).build());

            JsonNode json = serialize(CookeepsRecipeDetailResponseDto.from(recipe, false, false, false));

            assertThat(json.get("feature").isNull()).isTrue();
            assertThat(json.get("featureName").isNull()).isTrue();
        }

        @Test
        @DisplayName("viewer 상태 필드는 isLiked, isBookmarked, canLike, canBookmark 키로 직렬화된다")
        void viewer_상태_필드_JSON_키() throws Exception {
            DailyRecipe recipe = recipeWithFeature(Feature.RICE_BOWL);

            JsonNode json = serialize(CookeepsRecipeDetailResponseDto.from(recipe, true, false, false));

            assertThat(json.get("isLiked").asBoolean()).isTrue();
            assertThat(json.get("isBookmarked").asBoolean()).isFalse();
            assertThat(json.get("canLike").asBoolean()).isTrue();
            assertThat(json.get("canBookmark").asBoolean()).isTrue();
            assertThat(json.has("liked")).isFalse();
        }

        @Test
        @DisplayName("본인 레시피는 canLike, canBookmark가 false로 직렬화된다")
        void 본인_레시피_직렬화() throws Exception {
            DailyRecipe recipe = recipeWithFeature(Feature.RICE_BOWL);

            JsonNode json = serialize(CookeepsRecipeDetailResponseDto.from(recipe, false, false, true));

            assertThat(json.get("canLike").asBoolean()).isFalse();
            assertThat(json.get("canBookmark").asBoolean()).isFalse();
        }
    }
}
