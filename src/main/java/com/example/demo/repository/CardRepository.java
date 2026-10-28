package com.example.demo.repository;

import com.example.demo.entity.Card;
import com.example.demo.entity.CardLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    long countByDeckIdAndLocation(Long deckId, CardLocation location);
    long countByDeckIdAndOwnerIdAndLocation(Long deckId, Long ownerId, CardLocation location);

    @Query(value = """
        SELECT *
        FROM card
        WHERE deck_id = :deckId
          AND location = :location
        ORDER BY random()
        LIMIT 1
        FOR UPDATE
        """, nativeQuery = true)
    Optional<Card> findRandomCardForUpdate(@Param("deckId") Long deckId, @Param("location") String location);
}