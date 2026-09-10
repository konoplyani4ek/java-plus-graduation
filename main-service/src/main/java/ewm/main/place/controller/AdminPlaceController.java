package ewm.main.place.controller;

import ewm.main.place.Place;
import ewm.main.place.service.PlaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/admin/places")
@RequiredArgsConstructor
public class AdminPlaceController {

    private final PlaceService placeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Place create(@Valid @RequestBody Place place) {
        return placeService.create(place);
    }

    @PutMapping("/{placeId}")
    public Place update(@PathVariable long placeId,
                        @Valid @RequestBody Place place) {
        return placeService.update(placeId, place);
    }

    @DeleteMapping("/{placeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long placeId) {
        placeService.delete(placeId);
    }
}