package com.example.demo.service;

import com.example.demo.entity.Card;
import com.example.demo.entity.CardLocation;
import com.example.demo.entity.Deck;
import com.example.demo.entity.Ingredient;
import com.example.demo.entity.IngredientType;
import com.example.demo.entity.Recipe;
import com.example.demo.repository.IngredientRepository;
import com.example.demo.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class DeckCompositionService {
    private static final int MIN_ELEMENT_COPIES = 4;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;

    /**
     * Заполняет колоду картами на основе всех доступных рецептов.
     * Для обычных карт распределяет ELEMENT с гарантией минимума,
     * для мультикарт выбирает 3 разных ELEMENT
     */
    @Transactional
    public void populateDeck(Deck deck) {
        List<Recipe> recipes = recipeRepository.findAll();
        List<Ingredient> elements = ingredientRepository.findByType(IngredientType.ELEMENT);

        int normalCardCount = countNormalCards(recipes);
        if (elements.isEmpty() && normalCardCount > 0) {
            throw new IllegalStateException("Невозможно сформировать колоду: отсутствуют ELEMENT");
        }

        List<Ingredient> elementPool = createElementPool(elements, normalCardCount);
        int elementIndex = 0;

        for (Recipe recipe : recipes) {
            int copies = getCopies(recipe);

            for (int i = 0; i < copies; i++) {
                Card card = new Card();
                card.setDeck(deck);
                card.setRecipe(recipe);
                card.setLocation(CardLocation.DECK);

                if (isMultiIngredientCard(recipe)) {
                    card.getIngredients().addAll(selectDistinctElements(elements, 3));
                }
                else {
                    card.getIngredients().add(elementPool.get(elementIndex++));
                }

                deck.getCards().add(card);
            }
        }
    }

    /**
     * Подсчитывает количество карт, исключая мультикарты
     */
    private int countNormalCards(List<Recipe> recipes) {
        int count = 0;
        for (Recipe recipe : recipes) {
            int copies = getCopies(recipe);
            if (!isMultiIngredientCard(recipe)) {
                count += copies;
            }
        }
        return count;
    }

    /**
     * Создаёт пул ELEMENT для обычных карт.
     * Гарантирует минимум 4 карты каждого ELEMENT,
     * а оставшиеся позиции заполняет случайными ELEMENT
     */
    private List<Ingredient> createElementPool(List<Ingredient> elements, int normalCardCount) {
        int minimumRequired = elements.size() * MIN_ELEMENT_COPIES;
        if (normalCardCount < minimumRequired) {
            throw new IllegalStateException(
                    "Недостаточно обычных карт для формирования колоды: необходимо минимум " + minimumRequired
            );
        }

        List<Ingredient> pool = new ArrayList<>();
        for (Ingredient element : elements) {
            for (int i = 0; i < MIN_ELEMENT_COPIES; i++) {
                pool.add(element);
            }
        }

        while (pool.size() < normalCardCount) {
            Ingredient randomElement = elements.get(ThreadLocalRandom.current().nextInt(elements.size()));
            pool.add(randomElement);
        }

        //Перемешивает, чтобы гарантированные копии не шли подряд
        Collections.shuffle(pool);

        return pool;
    }

    /**
     * Выбирает указанное количество разных ELEMENT случайным образом для формирования мультикарт
     */
    private List<Ingredient> selectDistinctElements(List<Ingredient> elements, int count) {
        if (elements.size() < count) {
            throw new IllegalStateException(
                    "Недостаточно ELEMENT для мультикарты: необходимо " + count + ", доступно " + elements.size()
            );
        }

        List<Ingredient> shuffled = new ArrayList<>(elements);
        Collections.shuffle(shuffled);

        return new ArrayList<>(shuffled.subList(0, count));
    }

    /**
     * Определяет количество карт в колоде с указанным рецепта в зависимости от типа результата
     */
    private int getCopies(Recipe recipe) {
        IngredientType resultType = recipe.getResult().getType();

        return switch (resultType) {
            case SIMPLE_ELIXIR -> 2;
            case COMPLEX_ELIXIR -> 3;
            default -> 1;
        };
    }

    /**
     * Определяет, является ли карта мультикартой
     */
    private boolean isMultiIngredientCard(Recipe recipe) {
        IngredientType resultType = recipe.getResult().getType();
        return resultType == IngredientType.SUPREME_ELIXIR || resultType == IngredientType.GREAT_TALISMAN;
    }
}