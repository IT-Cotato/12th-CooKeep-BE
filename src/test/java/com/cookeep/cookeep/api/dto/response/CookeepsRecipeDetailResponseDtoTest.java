package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.user.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CookeepsRecipeDetailResponseDtoTest {

    @Test
    @DisplayName("viewer 상태 필드는 isLiked, isBookmarked, canLike, canBookmark 키로 직렬화된다")
    void viewer_상태_필드_JSON_키() throws Exception {
        User author = User.builder().userId(10L).nickname("작성자").build();
        DailyRecipe recipe = DailyRecipe.builder()
                .id(1L).title("된장찌개").content("{}").isPublic(true).likeCount(0).user(author).build();

        CookeepsRecipeDetailResponseDto dto = CookeepsRecipeDetailResponseDto.from(recipe, true, false, false);

        JsonNode json = new ObjectMapper().findAndRegisterModules().valueToTree(dto);

        assertThat(json.get("isLiked").asBoolean()).isTrue();
        assertThat(json.get("isBookmarked").asBoolean()).isFalse();
        assertThat(json.get("canLike").asBoolean()).isTrue();
        assertThat(json.get("canBookmark").asBoolean()).isTrue();
        assertThat(json.has("liked")).isFalse();
    }
}
