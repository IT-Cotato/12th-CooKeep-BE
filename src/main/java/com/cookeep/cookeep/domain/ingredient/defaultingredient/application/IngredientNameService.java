package com.cookeep.cookeep.domain.ingredient.defaultingredient.application;

import com.cookeep.cookeep.api.dto.response.IngredientNameResponseDto;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.dao.DefaultIngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IngredientNameService {

    private final DefaultIngredientRepository defaultIngredientRepository;

    @Transactional(readOnly = true)
   public IngredientNameResponseDto getIngredientNames() {
        List<IngredientNameResponseDto.IngredientItem> items = defaultIngredientRepository
                .findAllByOrderByIdAsc()
                .stream()
                .map(i -> new IngredientNameResponseDto.IngredientItem(i.getId(), i.getIngredient()))
                .toList();
        return new IngredientNameResponseDto(items);
    }
}
