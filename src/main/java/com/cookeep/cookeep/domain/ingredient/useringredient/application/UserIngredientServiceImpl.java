package com.cookeep.cookeep.domain.ingredient.useringredient.application;

import com.cookeep.cookeep.api.dto.request.UserIngredientCreateRequestDto;
import com.cookeep.cookeep.api.dto.response.UserIngredientCreateResponseDto;
import com.cookeep.cookeep.api.dto.response.UserIngredientListCreateResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.ingredient.common.domain.Storage;
import com.cookeep.cookeep.domain.ingredient.common.domain.Type;
import com.cookeep.cookeep.domain.ingredient.common.domain.Unit;
import com.cookeep.cookeep.domain.ingredient.customingredient.dao.CustomIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.customingredient.entity.CustomIngredient;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.dao.DefaultIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.entity.DefaultIngredient;
import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserIngredientServiceImpl implements UserIngredientService {

    private static final int DEFAULT_QUANTITY = 1;
    private static final Unit DEFAULT_CUSTOM_UNIT = Unit.PIECE;

    private final UserIngredientRepository userIngredientRepository;
    private final DefaultIngredientRepository defaultIngredientRepository;
    private final CustomIngredientRepository customIngredientRepository;
    private final UserRepository userRepository;

    @Override
    public UserIngredientListCreateResponseDto createAll(Long userId, List<UserIngredientCreateRequestDto> requests) {
        // 유저 존재 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<UserIngredientCreateResponseDto> results = requests.stream()
                .map(req -> createOne(user, req))
                .toList();

        return UserIngredientListCreateResponseDto.of(results);
    }

    private UserIngredientCreateResponseDto createOne(User user, UserIngredientCreateRequestDto req) {
        if (req.getType() == Type.DEFAULT) {
            return createFromDefault(user, req);
        } else {
            return createFromCustom(user, req);
        }
    }

    private UserIngredientCreateResponseDto createFromDefault(User user, UserIngredientCreateRequestDto req) {
        DefaultIngredient ref = defaultIngredientRepository.findById(req.getReferenceId())
                .orElseThrow(() -> new AppException(ErrorCode.INGREDIENT_REFERENCE_NOT_FOUND));

        int quantity = req.getQuantity()!= null ? req.getQuantity():DEFAULT_QUANTITY;
        Unit unit = req.getUnit()!= null ? req.getUnit():ref.getUnit();
        Storage storage = req.getStorage()!= null ? req.getStorage():ref.getDefaultStorage();
        LocalDate expiration = req.getExpirationDate()!= null ? req.getExpirationDate():calcExpiration(ref.getDefaultExpirationDays());
        String memo = req.getMemo();

        UserIngredient entity = UserIngredient.builder()
                .user(user)
                .type(Type.DEFAULT)
                .referenceId(ref.getId())
                .quantity(quantity)
                .unit(unit)
                .storage(storage)
                .expirationDate(expiration)
                .memo(memo)
                .build();

        userIngredientRepository.save(entity);

        return UserIngredientCreateResponseDto.of(entity, ref.getIngredient(), ref.getImageUrl());
    }

    private UserIngredientCreateResponseDto createFromCustom(User user, UserIngredientCreateRequestDto req) {
        CustomIngredient ref = customIngredientRepository.findById(req.getReferenceId())
                .orElseThrow(() -> new AppException(ErrorCode.INGREDIENT_REFERENCE_NOT_FOUND));

        int quantity = req.getQuantity()!= null ? req.getQuantity():DEFAULT_QUANTITY;
        Unit unit = req.getUnit()!= null ? req.getUnit():DEFAULT_CUSTOM_UNIT;
        Storage storage = req.getStorage()!= null ? req.getStorage():ref.getStorage();
        LocalDate expiration = req.getExpirationDate() != null ? req.getExpirationDate():calcExpiration(ref.getExpirationDays());
        String memo = req.getMemo();

        UserIngredient entity = UserIngredient.builder()
                .user(user)
                .type(Type.CUSTOM)
                .referenceId(ref.getId())
                .quantity(quantity)
                .unit(unit)
                .storage(storage)
                .expirationDate(expiration)
                .memo(memo)
                .build();

        userIngredientRepository.save(entity);

        return UserIngredientCreateResponseDto.of(entity, ref.getName(), ref.getImageUrl());
    }

    private LocalDate calcExpiration(Integer expirationDays) {
        if (expirationDays == null) return null;
        return LocalDate.now().plusDays(expirationDays);
    }

}
