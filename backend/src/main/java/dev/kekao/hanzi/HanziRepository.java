package dev.kekao.hanzi;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HanziRepository extends JpaRepository<HanziEntity, Long> {
    Optional<HanziEntity> findByCharacter(String character);
}
