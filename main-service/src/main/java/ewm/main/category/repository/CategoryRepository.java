package ewm.main.category.repository;

import ewm.main.category.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByName(String name);

    @Query("SELECT COUNT(c) FROM Category c WHERE NOT c.id = :id AND c.name = :name")
    long countByNameExcludingId(@Param("id") Long id, @Param("name") String name);

    @Query(value = "SELECT * FROM categories c ORDER BY c.id LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<Category> findAllWithLimitOffset(@Param("offset") Integer offset,
                                          @Param("limit") Integer limit);
}
