package dev.kekao.deck;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserDeckRepository extends JpaRepository<UserDeckEntity, UserDeckId> {
    List<UserDeckEntity> findByUserId(Long userId);

    @Query("select ud.deck.id from UserDeckEntity ud where ud.user.id = :userId")
    List<Long> findDeckIdsByUserId(@Param("userId") Long userId);
}
