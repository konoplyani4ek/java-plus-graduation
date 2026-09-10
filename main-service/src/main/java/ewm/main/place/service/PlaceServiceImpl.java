package ewm.main.place.service;

import ewm.main.dto.search.PageParam;
import ewm.main.exception.NotFoundException;
import ewm.main.place.Place;
import ewm.main.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceServiceImpl implements PlaceService {
    private final PlaceRepository placeRepository;

    @Override
    public Place create(Place place) {
        place.setId(null);
        return placeRepository.save(place);
    }

    @Override
    public Place update(long placeId, Place place) {
        Place existing = getById(placeId);

        existing.setName(place.getName());
        existing.setLat(place.getLat());
        existing.setLon(place.getLon());

        return placeRepository.save(existing);
    }

    @Override
    public void delete(long placeId) {
        Place place = getById(placeId);
        placeRepository.delete(place);
    }

    @Override
    public List<Place> getAll(PageParam pageParam) {
        PageRequest pageRequest = PageRequest.of(
                pageParam.getFrom() / pageParam.getSize(),
                pageParam.getSize()
        );

        return placeRepository.findAll(pageRequest).getContent();
    }

    @Override
    public Place getById(long placeId) {
        return placeRepository.findById(placeId)
                .orElseThrow(() -> new NotFoundException("Локация не найдена: " + placeId));
    }
}