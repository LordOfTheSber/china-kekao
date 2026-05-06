package dev.kekao.hanzi;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HanziRepository extends JpaRepository<HanziEntity, Long> {
    Optional<HanziEntity> findByCharacter(String character);

    Page<HanziEntity> findAllByStatus(HanziStatus status, Pageable pageable);

    @Query("""
        select h from HanziEntity h
        where h.hskLevel between :minLevel and :maxLevel
        """)
    List<HanziEntity> findAllByHskLevelBetween(@Param("minLevel") short minLevel, @Param("maxLevel") short maxLevel);
}
