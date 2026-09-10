package ewm.user.client;

import ewm.user.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Внутренний клиент к user-service.
 * Ходит на отдельный префикс /internal/**, который НЕ проксируется через Gateway наружу —
 * это приватный контракт между микросервисами, а не публичный API.
 */
@FeignClient(name = "user-service", path = "/internal/users")
public interface UserClient {

    @GetMapping("/{userId}")
    UserDto getUser(@PathVariable("userId") long userId);

    /**
     * Пакетное получение пользователей по списку id — позволяет собирать
     * EventFullDto/EventShortDto для списка событий без N+1 обращений к user-service.
     */
    @GetMapping
    List<UserDto> getUsers(@RequestParam("ids") List<Long> ids);
}