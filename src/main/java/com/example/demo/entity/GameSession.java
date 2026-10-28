package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "game_sessions")
@Getter
@Setter
@NoArgsConstructor
public class GameSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lobby_id", nullable = false, unique = true)
    private Lobby lobby;

    @OneToOne(
            mappedBy = "gameSession",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Deck deck;

    @OneToMany(
            mappedBy = "gameSession",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("id ASC")
    private List<GameSessionPlayer> players = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameSessionStatus status;

    @Column(nullable = false)
    private LocalDateTime startedAt;
}