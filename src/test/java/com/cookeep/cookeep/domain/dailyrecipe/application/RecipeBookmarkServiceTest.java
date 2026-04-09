package com.cookeep.cookeep.domain.dailyrecipe.application;

import com.cookeep.cookeep.api.dto.response.CookeepsFeedResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.dailyrecipe.dao.DailyRecipeRepository;
import com.cookeep.cookeep.domain.dailyrecipe.dao.RecipeBookmarkRepository;
import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.dailyrecipe.entity.RecipeBookmark;
import com.cookeep.cookeep.domain.user.application.UserReader;
import com.cookeep.cookeep.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecipeBookmarkServiceTest {

    @Mock
    private RecipeBookmarkRepository recipeBookmarkRepository;

    @Mock
    private DailyRecipeRepository dailyRecipeRepository;

    @Mock
    private UserReader userReader;

    @InjectMocks
    private RecipeBookmarkService recipeBookmarkService;

    private User bookmarker;
    private User recipeOwner;
    private DailyRecipe recipe;

    @BeforeEach
    void setUp() {
        bookmarker = mock(User.class);
        recipeOwner = mock(User.class);
        given(bookmarker.getUserId()).willReturn(1L);
        given(recipeOwner.getUserId()).willReturn(2L);

        recipe = DailyRecipe.builder()
                .title("테스트 레시피")
                .content("{}")
                .isPublic(true)
                .user(recipeOwner)
                .build();
    }

    @Nested
    @DisplayName("toggleBookmark - 북마크 추가")
    class BookmarkAdd {

        @BeforeEach
        void setUp() {
            given(userReader.readById(1L)).willReturn(bookmarker);
            given(dailyRecipeRepository.findById(100L)).willReturn(Optional.of(recipe));
            given(recipeBookmarkRepository.findByDailyRecipeAndUser(recipe, bookmarker)).willReturn(Optional.empty());
            given(recipeBookmarkRepository.save(any(RecipeBookmark.class))).willAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("북마크 추가 시 true를 반환한다")
        void 북마크추가_true반환() {
            boolean result = recipeBookmarkService.toggleBookmark(1L, 100L);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("북마크 추가 시 저장을 호출한다")
        void 북마크추가_저장_호출() {
            recipeBookmarkService.toggleBookmark(1L, 100L);

            verify(recipeBookmarkRepository).save(any(RecipeBookmark.class));
        }
    }

    @Nested
    @DisplayName("toggleBookmark - 북마크 취소")
    class BookmarkCancel {

        private RecipeBookmark existingBookmark;

        @BeforeEach
        void setUp() {
            existingBookmark = RecipeBookmark.builder().dailyRecipe(recipe).user(bookmarker).build();

            given(userReader.readById(1L)).willReturn(bookmarker);
            given(dailyRecipeRepository.findById(100L)).willReturn(Optional.of(recipe));
            given(recipeBookmarkRepository.findByDailyRecipeAndUser(recipe, bookmarker))
                    .willReturn(Optional.of(existingBookmark));
        }

        @Test
        @DisplayName("북마크 취소 시 false를 반환한다")
        void 북마크취소_false반환() {
            boolean result = recipeBookmarkService.toggleBookmark(1L, 100L);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("북마크 취소 시 삭제를 호출한다")
        void 북마크취소_삭제_호출() {
            recipeBookmarkService.toggleBookmark(1L, 100L);

            verify(recipeBookmarkRepository).delete(existingBookmark);
        }
    }

    @Nested
    @DisplayName("toggleBookmark - 예외 케이스")
    class BookmarkException {

        @Test
        @DisplayName("자신의 레시피에 북마크를 하면 예외가 발생한다")
        void 자신의레시피_북마크_예외() {
            given(userReader.readById(2L)).willReturn(recipeOwner);
            given(dailyRecipeRepository.findById(100L)).willReturn(Optional.of(recipe));

            assertThatThrownBy(() -> recipeBookmarkService.toggleBookmark(2L, 100L))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.CANNOT_BOOKMARK_OWN_RECIPE.getMessage());
        }

        @Test
        @DisplayName("존재하지 않는 레시피를 북마크하면 예외가 발생한다")
        void 없는레시피_북마크_예외() {
            given(userReader.readById(1L)).willReturn(bookmarker);
            given(dailyRecipeRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> recipeBookmarkService.toggleBookmark(1L, 999L))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.DAILY_RECIPE_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("getMyBookmarkedRecipes - 내가 북마크한 레시피 목록 조회")
    class GetMyBookmarkedRecipes {

        private DailyRecipe bookmarkedRecipe;
        private Pageable pageable;

        @BeforeEach
        void setUp() {
            pageable = PageRequest.of(0, 10);
            bookmarkedRecipe = DailyRecipe.builder()
                    .title("북마크한 레시피")
                    .content("{}")
                    .isPublic(true)
                    .likeCount(3)
                    .recipeImageUrl("http://image.url")
                    .user(recipeOwner)
                    .build();
            ReflectionTestUtils.setField(bookmarkedRecipe, "id", 20L);
            ReflectionTestUtils.setField(bookmarkedRecipe, "createdAt", LocalDateTime.of(2024, 6, 1, 9, 0));

            given(userReader.readById(1L)).willReturn(bookmarker);
        }

        @Test
        @DisplayName("레시피 목록을 CookeepsFeedResponseDto의 필드에 올바르게 매핑한다")
        void 목록조회_DTO_필드_매핑() {
            Slice<DailyRecipe> slice = new SliceImpl<>(List.of(bookmarkedRecipe), pageable, false);
            given(recipeBookmarkRepository.findMyBookmarkedRecipes(bookmarker, pageable)).willReturn(slice);

            Slice<CookeepsFeedResponseDto> result = recipeBookmarkService.getMyBookmarkedRecipes(1L, pageable);

            assertThat(result.getContent()).hasSize(1);
            CookeepsFeedResponseDto dto = result.getContent().get(0);
            assertThat(dto.getDailyRecipeId()).isEqualTo(20L);
            assertThat(dto.getTitle()).isEqualTo("북마크한 레시피");
            assertThat(dto.getLikeCount()).isEqualTo(3);
            assertThat(dto.getRecipeImageUrl()).isEqualTo("http://image.url");
            assertThat(dto.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 6, 1, 9, 0));
        }

        @Test
        @DisplayName("다음 페이지가 있으면 last=false를 반환한다")
        void 다음페이지있으면_last_false() {
            Slice<DailyRecipe> slice = new SliceImpl<>(List.of(bookmarkedRecipe), pageable, true);
            given(recipeBookmarkRepository.findMyBookmarkedRecipes(bookmarker, pageable)).willReturn(slice);

            Slice<CookeepsFeedResponseDto> result = recipeBookmarkService.getMyBookmarkedRecipes(1L, pageable);

            assertThat(result.isLast()).isFalse();
        }

        @Test
        @DisplayName("마지막 페이지이면 last=true를 반환한다")
        void 마지막페이지이면_last_true() {
            Slice<DailyRecipe> slice = new SliceImpl<>(List.of(bookmarkedRecipe), pageable, false);
            given(recipeBookmarkRepository.findMyBookmarkedRecipes(bookmarker, pageable)).willReturn(slice);

            Slice<CookeepsFeedResponseDto> result = recipeBookmarkService.getMyBookmarkedRecipes(1L, pageable);

            assertThat(result.isLast()).isTrue();
        }

        @Test
        @DisplayName("북마크한 레시피가 없으면 빈 Slice를 반환한다")
        void 북마크레시피없으면_빈Slice() {
            Slice<DailyRecipe> emptySlice = new SliceImpl<>(List.of(), pageable, false);
            given(recipeBookmarkRepository.findMyBookmarkedRecipes(bookmarker, pageable)).willReturn(emptySlice);

            Slice<CookeepsFeedResponseDto> result = recipeBookmarkService.getMyBookmarkedRecipes(1L, pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.isLast()).isTrue();
        }
    }
}
