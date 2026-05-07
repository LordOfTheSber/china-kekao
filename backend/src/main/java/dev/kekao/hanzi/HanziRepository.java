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

    List<HanziEntity> findAllByStatus(HanziStatus status);

    List<HanziEntity> findAllByStatusAndHskLevelOrderByFrequencyRankAscIdAsc(HanziStatus status, Short hskLevel);

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

    /**
     * Full-text-ish search across character, pinyin and English meanings of PUBLISHED
     * hanzi, ordered by relevance (exact character match > prefix on pinyin > rest)
     * and {@code frequency_rank} ascending.
     */
    @Query(value = """
            SELECT h.*
            FROM hanzi h
            LEFT JOIN hanzi_translation t
                   ON t.hanzi_id = h.id AND t.language = 'en'
            WHERE h.status = 'PUBLISHED'
              AND (:hsk IS NULL OR h.hsk_level = :hsk)
              AND (
                  :q = ''
                  OR h.character = :q
                  OR h.pinyin ILIKE :qLike
                  OR EXISTS (
                      SELECT 1 FROM unnest(COALESCE(t.meanings, ARRAY[]::text[])) m
                      WHERE m ILIKE :qLike
                  )
              )
            ORDER BY
              CASE
                WHEN :q <> '' AND h.character = :q THEN 0
                WHEN :q <> '' AND h.pinyin ILIKE :qPrefix THEN 1
                WHEN :q <> '' AND h.pinyin ILIKE :qLike   THEN 2
                ELSE 3
              END ASC,
              COALESCE(h.frequency_rank, 1000000) ASC,
              h.id ASC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<HanziEntity> searchPublished(@Param("q") String q,
                                      @Param("qPrefix") String qPrefix,
                                      @Param("qLike") String qLike,
                                      @Param("hsk") Short hsk,
                                      @Param("limit") int limit,
                                      @Param("offset") int offset);

    @Query(value = """
            SELECT COUNT(DISTINCT h.id)
            FROM hanzi h
            LEFT JOIN hanzi_translation t
                   ON t.hanzi_id = h.id AND t.language = 'en'
            WHERE h.status = 'PUBLISHED'
              AND (:hsk IS NULL OR h.hsk_level = :hsk)
              AND (
                  :q = ''
                  OR h.character = :q
                  OR h.pinyin ILIKE :qLike
                  OR EXISTS (
                      SELECT 1 FROM unnest(COALESCE(t.meanings, ARRAY[]::text[])) m
                      WHERE m ILIKE :qLike
                  )
              )
            """, nativeQuery = true)
    long countSearchPublished(@Param("q") String q,
                              @Param("qLike") String qLike,
                              @Param("hsk") Short hsk);
}
