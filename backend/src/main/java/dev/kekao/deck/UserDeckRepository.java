package dev.kekao.deck;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserDeckRepository extends JpaRepository<UserDeckEntity, UserDeckId> {
    List<UserDeckEntity> findByUserId(Long userId);
}
