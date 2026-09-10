package ewm.main.place.service;

import ewm.main.dto.search.PageParam;
import ewm.main.place.Place;

import java.util.List;

public interface PlaceService {

    Place create(Place place);

    Place update(long placeId, Place place);

    void delete(long placeId);

    List<Place> getAll(PageParam pageParam);

    Place getById(long placeId);
}