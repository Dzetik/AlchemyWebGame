
package com.example.demo.service;

import com.example.demo.dto.game.CardDrawResponse;
import com.example.demo.entity.*;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ForbiddenException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.CardRepository;
import com.example.demo.repository.DeckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardDrawService {
    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;
    private final GameSessionAccessService gameSessionAccessService;

    /**
     * Проверяет доступ игрока и получает колоду, затем вызывает общий метод выдачи карты
     * @param gameId
     * @param userId
     * @return
     */
    @Transactional
    public CardDrawResponse drawCard(Long gameId, Long userId) {
        GameSession gameSession = gameSessionAccessService.getGameSessionForPlayer(gameId, userId);
        User player = findPlayer(gameSession, userId);
        Deck deck = getDeck(gameSession);

        return toResponse(takeCard(deck, player));
    }

    /**
     * Рассчитывает количество недостающих карт и управляет циклом
     * @param gameSession
     * @param player
     * @return
     */
    @Transactional
    public List<CardDrawResponse> drawCardsToInitialHand(GameSession gameSession, User player) {
        Deck deck = getDeck(gameSession);
        int targetCards = gameSession.getLobby().getInitialHandCards();
        long cardsInHand = cardRepository.countByDeckIdAndOwnerIdAndLocation(
                deck.getId(),
                player.getId(),
                CardLocation.HAND
        );

        int cardsToDraw = (int) Math.max(0L, targetCards - cardsInHand);

        List<CardDrawResponse> drawnCards = new ArrayList<>(cardsToDraw);

        for (int i = 0; i < cardsToDraw; i++) {
            // при пустой колоде прекращает добор
            var cardOptional = cardRepository.findRandomCardForUpdate(
                    deck.getId(),
                    CardLocation.DECK.name()
            );

            if (cardOptional.isEmpty()) {
                break;
            }

            Card card = cardOptional.get();
            assignToHand(card, player);

            drawnCards.add(toResponse(card));
        }

        return drawnCards;
    }

    private Deck getDeck(GameSession gameSession) {
        return deckRepository.findByGameSessionId(gameSession.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Колода для игровой сессии с id "
                                + gameSession.getId() + " не найдена"
                ));
    }

    private User findPlayer(GameSession gameSession, Long userId) {
        return gameSession.getPlayers().stream()
                .map(GameSessionPlayer::getUser)
                .filter(user -> user.getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException(
                        "Пользователь не является участником этой игры"
                ));
    }

    private Card takeCard(Deck deck, User player) {
        Card card = cardRepository.findRandomCardForUpdate(
                        deck.getId(),
                        CardLocation.DECK.name()
                )
                .orElseThrow(() -> new ConflictException(
                        "В колоде не осталось карт"
                ));

        assignToHand(card, player);
        return card;
    }

    private void assignToHand(Card card, User player) {
        card.setLocation(CardLocation.HAND);
        card.setOwner(player);
    }

    private CardDrawResponse toResponse(Card card) {
        return new CardDrawResponse(
                card.getId(),
                card.getRecipe() != null ? card.getRecipe().getId() : null,
                card.getLocation()
        );
    }
}
