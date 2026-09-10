package ewm.main.compilation.repository;

import ewm.main.compilation.model.Compilation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    @Query("SELECT COUNT(c) FROM Compilation c WHERE NOT c.id = :id AND c.title = :title")
    long countByTitleExcludingId(@Param("id") Long id, @Param("title") String title);

    @Query("SELECT COUNT(c) FROM Compilation c WHERE c.title = :title")
    long countByTitle(@Param("title") String title);

    @Query(value = "SELECT * FROM compilations c WHERE c.pinned = :pinned ORDER BY id LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<Compilation> findAllWithLimitOffset(@Param("offset") Integer offset,
                                             @Param("limit") Integer limit,
                                             @Param("pinned") Boolean pinned
    );

    @Query(value = "SELECT * FROM compilations c ORDER BY id LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<Compilation> findAllWithLimitOffset(@Param("offset") Integer offset,
                                             @Param("limit") Integer limit
    );

    @EntityGraph(attributePaths = {
            "events",
            "events.category",
            "events.initiator"
    })
    List<Compilation> findByIdInOrderByIdAsc(List<Long> ids);
}
