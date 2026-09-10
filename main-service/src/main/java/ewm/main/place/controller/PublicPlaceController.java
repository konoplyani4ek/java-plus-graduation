package ewm.main.place.controller;

import ewm.main.dto.search.PageParam;
import ewm.main.place.Place;
import ewm.main.place.service.PlaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/places")
@RequiredArgsConstructor
public class PublicPlaceController {
    private final PlaceService placeService;

    @GetMapping
    public List<Place> getAll(@Valid @ModelAttribute PageParam pageParam) {
        return placeService.getAll(pageParam);
    }

    @GetMapping("/{placeId}")
    public Place getById(@PathVariable long placeId) {
        return placeService.getById(placeId);
    }
}