package com.example.demo;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.service.DeckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jwt.secret=test-secret-key-must-be-at-least-32-characters-long"
})
class DeckIntegrationTest {
    @Autowired
    private DeckService deckService;
    @Autowired
    private CardRepository cardRepository;
    @Autowired
    private DeckRepository deckRepository;
    @Autowired
    private GameSessionRepository gameSessionRepository;
    @Autowired
    private LobbyRepository lobbyRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private IngredientRepository ingredientRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        cardRepository.deleteAll();
        deckRepository.deleteAll();
        gameSessionRepository.deleteAll();
        lobbyRepository.deleteAll();
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void createDeck_shouldCreateCardsAccordingToRecipeTypes() {
        User owner = createUser(
                "Owner",
                "owner@test.com"
        );

        User player = createUser(
                "Player",
                "player@test.com"
        );

        Lobby lobby = new Lobby(
                "Test Lobby",
                owner,
                2
        );
        lobby.getPlayers().add(player);
        lobby = lobbyRepository.save(lobby);

        Ingredient stone = createIngredient(
                "Камень",
                IngredientType.ELEMENT
        );
        Ingredient light = createIngredient(
                "Свет",
                IngredientType.ELEMENT
        );
        Ingredient water = createIngredient(
                "Вода",
                IngredientType.ELEMENT
        );
        Ingredient air = createIngredient(
                "Воздух",
                IngredientType.ELEMENT
        );

        Ingredient simpleResult = createIngredient(
                "Простой эликсир",
                IngredientType.SIMPLE_ELIXIR
        );

        Ingredient complexResult = createIngredient(
                "Сложный эликсир",
                IngredientType.COMPLEX_ELIXIR
        );

        Ingredient greatResult = createIngredient(
                "Великий эликсир",
                IngredientType.GREAT_ELIXIR
        );

        Recipe simpleRecipe = createRecipe(simpleResult);
        Recipe complexRecipe = createRecipe(complexResult);
        Recipe greatRecipe = createRecipe(greatResult);

        for (int i = 1; i <= 10; i++) {
            Ingredient result = createIngredient(
                    "Дополнительный результат " + i,
                    IngredientType.GREAT_ELIXIR
            );

            createRecipe(result);
        }

        GameSession gameSession = new GameSession();
        gameSession.setLobby(lobby);
        gameSession.setPlayers(List.of(owner, player));
        gameSession.setStatus(GameSessionStatus.ACTIVE);
        gameSession.setStartedAt(LocalDateTime.now());

        gameSession = gameSessionRepository.save(gameSession);

        Deck deck = deckService.createDeck(gameSession);

        List<Card> cards = cardRepository.findAll();

        assertThat(deckRepository.findByGameSessionId(gameSession.getId()))
                .isPresent();

        assertThat(cards)
                .hasSize(16);

        assertThat(cards)
                .allMatch(card ->
                        card.getDeck().getId().equals(deck.getId())
                );

        assertThat(cards)
                .allMatch(card ->
                        card.getLocation() == CardLocation.DECK
                );

        assertThat(cards.stream()
                .filter(card -> card.getRecipe().getId().equals(simpleRecipe.getId()))
                .count())
                .isEqualTo(2);

        assertThat(cards.stream()
                .filter(card -> card.getRecipe().getId().equals(complexRecipe.getId()))
                .count())
                .isEqualTo(3);

        assertThat(cards.stream()
                .filter(card -> card.getRecipe().getId().equals(greatRecipe.getId()))
                .count())
                .isEqualTo(1);

        assertThat(cards)
                .allMatch(card -> !card.getIngredients().isEmpty());

        assertThat(cards)
                .allMatch(card ->
                        card.getIngredients()
                                .stream()
                                .allMatch(ingredient ->
                                        ingredient.getType() == IngredientType.ELEMENT
                                )
                );
    }

    private User createUser(String name, String email) {
        User user = new User(
                name,
                email,
                "password"
        );
        user.setRole(Role.USER);

        return userRepository.save(user);
    }

    private Ingredient createIngredient(
            String name,
            IngredientType type
    ) {
        Ingredient ingredient = new Ingredient();
        ingredient.setName(name);
        ingredient.setType(type);

        return ingredientRepository.save(ingredient);
    }

    private Recipe createRecipe(Ingredient result) {
        Recipe recipe = new Recipe();
        recipe.setResult(result);
        recipe.setScore(1);

        return recipeRepository.save(recipe);
    }
}