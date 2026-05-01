package dev.kekao.hanzi;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HanziExampleRepository extends JpaRepository<HanziExampleEntity, Long> {
    List<HanziExampleEntity> findByHanziId(Long hanziId);
}
