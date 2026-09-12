package ewm.place.server.service;

import ewm.place.server.dto.search.PageParam;
import ewm.place.server.model.Place;

import java.util.List;

public interface PlaceService {

    Place create(Place place);

    Place update(long placeId, Place place);

    void delete(long placeId);

    List<Place> getAll(PageParam pageParam);

    Place getById(long placeId);
}