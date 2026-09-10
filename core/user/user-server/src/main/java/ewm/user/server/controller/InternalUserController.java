package ewm.user.server.controller;

import ewm.user.dto.UserDto;
import ewm.user.server.mapper.UserMapper;
import ewm.user.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public UserDto getUser(@PathVariable long userId) {
        return UserMapper.toDto(userService.getUserOrThrow(userId));
    }

    @GetMapping
    public List<UserDto> getUsers(@RequestParam List<Long> ids) {
        return userService.findUsersByIds(ids).stream().map(UserMapper::toDto).toList();
    }
}