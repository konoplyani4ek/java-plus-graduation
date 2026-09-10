package ewm.additional.server.place.service;

import ewm.additional.server.dto.search.PageParam;
import ewm.additional.server.place.model.Place;

import java.util.List;

public interface PlaceService {

    Place create(Place place);

    Place update(long placeId, Place place);

    void delete(long placeId);

    List<Place> getAll(PageParam pageParam);

    Place getById(long placeId);
}