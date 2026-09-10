package ewm.user.server.service;

import ewm.user.server.exception.ConflictException;
import ewm.user.server.exception.NotFoundException;
import ewm.user.server.model.User;
import ewm.user.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> findUsersByIds(List<Long> ids) {
        return userRepository.findAllById(ids);
    }

    public List<User> findAllUsers(int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        return userRepository.findAll(pageable).getContent();
    }

    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ConflictException("Такой email уже используется!");
        }
        User saved = userRepository.save(user);
        log.info("User created: {}", saved.getId());
        return saved;
    }

    public User getUserOrThrow(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User с таким id не найден");
        }
        userRepository.deleteById(id);
    }
}