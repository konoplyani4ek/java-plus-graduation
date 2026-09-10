package ewm.main.user;

import ewm.main.dto.UserShortDto;

public class UserMapper {
    public static UserShortDto toUserShortDto(User user) {
        return new UserShortDto(user.getId(), user.getEmail());
    }
}
