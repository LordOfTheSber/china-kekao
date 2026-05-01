package dev.kekao.hanzi;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HanziTranslationRepository extends JpaRepository<HanziTranslationEntity, Long> {
    List<HanziTranslationEntity> findByHanziId(Long hanziId);
}
