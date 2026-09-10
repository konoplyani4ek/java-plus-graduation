package ewm.user.server.repository;

import ewm.user.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    // findAllById(Iterable<Long> ids) уже есть в JpaRepository — используем его напрямую
}