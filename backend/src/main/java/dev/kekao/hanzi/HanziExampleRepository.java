package dev.kekao.hanzi;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HanziExampleRepository extends JpaRepository<HanziExampleEntity, Long> {
    List<HanziExampleEntity> findByHanziId(Long hanziId);

    @Query("""
            SELECT he FROM HanziExampleEntity he
            WHERE he.hanzi.id = :hanziId AND he.language = :language
            """)
    List<HanziExampleEntity> findByHanziIdAndLanguage(@Param("hanziId") Long hanziId,
                                                      @Param("language") String language);
}
