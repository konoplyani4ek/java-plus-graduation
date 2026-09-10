package ewm.main.event.mapper;

import ewm.main.dto.LocationDto;
import ewm.main.event.model.Location;

public class LocationMapper {

    public static Location toLocation(LocationDto locationDto) {
        return locationDto != null
                ? new Location(locationDto.getLat(), locationDto.getLon())
                : null;
    }

    public static LocationDto toLocationDto(Location location) {
        return location != null ?
                new LocationDto(location.getLat(), location.getLon())
                : null;
    }

}
