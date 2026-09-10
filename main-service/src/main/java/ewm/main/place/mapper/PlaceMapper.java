package ewm.main.place.mapper;

import ewm.main.dto.ShortPlaceDto;
import ewm.main.place.Place;

public class PlaceMapper {

    public static ShortPlaceDto toShortDto(Place place) {
        if (place == null) {
            return null;
        }

        return ShortPlaceDto.builder()
                .id(place.getId())
                .name(place.getName())
                .build();
    }
}