package dev.kekao.hanzi;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface HanziTranslationRepository extends JpaRepository<HanziTranslationEntity, Long> {

    @Query("""
            SELECT ht FROM HanziTranslationEntity ht
            WHERE ht.hanzi.id = :hanziId
            """)
    List<HanziTranslationEntity> findByHanziId(@Param("hanziId") Long hanziId);

    /**
     * Returns translations for the given hanzi ids and language. Uses a JOIN FETCH on the
     * {@code hanzi} association so callers iterating the result and reading {@code getHanzi().getId()}
     * do not trigger an extra query per row (N+1).
     */
    @Query("""
            SELECT ht FROM HanziTranslationEntity ht
            JOIN FETCH ht.hanzi
            WHERE ht.hanzi.id IN :hanziIds AND ht.language = :language
            """)
    List<HanziTranslationEntity> findByHanziIdInAndLanguage(@Param("hanziIds") Collection<Long> hanziIds,
                                                            @Param("language") String language);

    @Query("""
            SELECT ht.meanings FROM HanziTranslationEntity ht
            WHERE ht.hanzi.id = :hanziId AND ht.language = :language
            """)
    List<List<String>> findMeaningsByHanziIdAndLanguage(@Param("hanziId") Long hanziId,
                                                        @Param("language") String language);
}
