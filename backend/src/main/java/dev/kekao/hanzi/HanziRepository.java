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

    /**
     * Random distractor candidates: published hanzi that share an HSK level with the
     * target, excluding the target itself. Used by the CHOICE production fallback
     * (TASK-024). Falls back to any published hanzi if not enough same-level candidates.
     */
    @Query(value = """
            SELECT * FROM hanzi h
            WHERE h.status = 'PUBLISHED'
              AND h.id <> :hanziId
              AND (:hskLevel IS NULL OR h.hsk_level = :hskLevel)
            ORDER BY random()
            LIMIT :count
            """, nativeQuery = true)
    List<HanziEntity> findRandomDistractors(@Param("hanziId") Long hanziId,
                                            @Param("hskLevel") Short hskLevel,
                                            @Param("count") int count);

    @Query(value = """
            SELECT * FROM hanzi h
            WHERE h.status = 'PUBLISHED'
              AND h.id <> :hanziId
              AND h.id NOT IN (:excludeIds)
            ORDER BY random()
            LIMIT :count
            """, nativeQuery = true)
    List<HanziEntity> findRandomDistractorsExcluding(@Param("hanziId") Long hanziId,
                                                     @Param("excludeIds") List<Long> excludeIds,
                                                     @Param("count") int count);
}
