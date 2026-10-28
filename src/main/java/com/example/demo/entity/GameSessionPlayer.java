package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "game_session_players",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"game_session_id", "user_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class GameSessionPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSession gameSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public GameSessionPlayer(GameSession gameSession, User user) {
        this.gameSession = gameSession;
        this.user = user;
    }
}