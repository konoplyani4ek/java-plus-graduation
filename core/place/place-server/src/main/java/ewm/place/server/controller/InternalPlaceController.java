package ewm.place.server.controller;

import ewm.place.server.repository.PlaceRepository;
import ewm.place.server.exception.NotFoundException;
import ewm.place.server.model.Place;
import ewm.place.dto.PlaceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Внутренний (межсервисный) контроллер — потребитель PlaceClient (сейчас: event-service).
 * НЕ проксируется через Gateway.
 */
@RestController
@RequestMapping("/internal/places")
@RequiredArgsConstructor
public class InternalPlaceController {

    private final PlaceRepository placeRepository;

    @GetMapping("/{placeId}")
    public PlaceDto getPlace(@PathVariable long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new NotFoundException("Место с id=" + placeId + " не найдено"));

        return PlaceDto.builder()
                .id(place.getId())
                .name(place.getName())
                .lat(place.getLat())
                .lon(place.getLon())
                .build();
    }
}