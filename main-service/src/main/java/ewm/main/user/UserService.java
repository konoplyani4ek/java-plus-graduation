package ewm.main.user;

import ewm.main.exception.ConflictException;
import ewm.main.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    @Autowired
    private final UserRepository userRepository;

    public List<User> findUsersByIds(List<Long> ids) {
        return userRepository.findAllById(ids);
    }

    public List<User> findAllUsers(int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        // from-количество пропускаемых в начале списка элементов
        //size-общее количество элементов
        return userRepository.findAll(pageable).getContent();
    }

    public User createUser(User user) {
        log.info("Attempting to create user: {}", user); // логируем входные данные

        if (userRepository.existsByEmail(user.getEmail())) {
            log.warn("Conflict: email already exists: {}", user.getEmail());
            throw new ConflictException("Такой email уже используется!");
        }

        try {
            User savedUser = userRepository.save(user);
            log.info("User created successfully: {}", savedUser.getId());
            return savedUser;
        } catch (Exception e) {
            log.error("Failed to save user: ", e); // ловим и логируем ошибки JPA
            throw e;
        }
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User с таким id не найден");
        } else {
            userRepository.deleteById(id);
        }
    }
}
