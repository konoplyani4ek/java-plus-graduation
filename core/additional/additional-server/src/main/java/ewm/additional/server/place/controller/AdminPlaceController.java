package ewm.additional.server.place.controller;

import ewm.additional.server.place.model.Place;
import ewm.additional.server.place.service.PlaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    public Place update(@PathVariable long placeId, @Valid @RequestBody Place place) {
        return placeService.update(placeId, place);
    }

    @DeleteMapping("/{placeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long placeId) {
        placeService.delete(placeId);
    }
}