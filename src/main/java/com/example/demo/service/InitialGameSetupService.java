package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.CardRepository;
import com.example.demo.repository.DeckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InitialGameSetupService {
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;

    @Transactional
    public void prepare(GameSession gameSession) {
        Deck deck = deckRepository.findByGameSessionId(gameSession.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Колода для игровой сессии с id " + gameSession.getId() + " не найдена"
                ));

        int initialHandCards = gameSession.getLobby().getInitialHandCards();
        int initialTableCards = gameSession.getLobby().getInitialTableCards();

        int requiredCards = gameSession.getPlayers().size() * initialHandCards + initialTableCards;
        long availableCards = cardRepository.countByDeckIdAndLocation(deck.getId(), CardLocation.DECK);
        if (availableCards < requiredCards) {
            throw new ConflictException(
                    "Недостаточно карт для начала игры. " + "Требуется: " + requiredCards + ", доступно: " + availableCards
            );
        }

        for (GameSessionPlayer sessionPlayer : gameSession.getPlayers()) {
            User player = sessionPlayer.getUser();
            for (int i = 0; i < initialHandCards; i++) {
                Card card = takeDeckCard(deck.getId());
                card.setLocation(CardLocation.HAND);
                card.setOwner(player);
            }
        }

        for (int i = 0; i < initialTableCards; i++) {
            Card card = takeDeckCard(deck.getId());
            card.setLocation(CardLocation.TABLE);
            card.setOwner(null);
        }
    }

    private Card takeDeckCard(Long deckId) {
        return cardRepository.findRandomCardForUpdate(deckId, CardLocation.DECK.name())
                .orElseThrow(() -> new ConflictException(
                        "В колоде не осталось карт"
                ));
    }
}
