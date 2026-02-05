package com.cookeep.cookeep.domain.refrigerator.application;

import com.cookeep.cookeep.domain.ingredient.customingredient.dao.CustomIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.dao.DefaultIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefigeratorService {

    private final UserIngredientRepository userIngredientRepository;
    private final DefaultIngredientRepository defaultIngredientRepository;
    private final CustomIngredientRepository customIngredientRepository;


}
