package ewm.user.server.mapper;


import ewm.user.dto.NewUserRequestDto;
import ewm.user.dto.UserDto;
import ewm.user.server.model.User;

public class UserMapper {

    private UserMapper() {
    }

    public static User toEntity(NewUserRequestDto dto) {
        return User.builder()
                .email(dto.getEmail())
                .name(dto.getName())
                .build();
    }

    public static UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }
}