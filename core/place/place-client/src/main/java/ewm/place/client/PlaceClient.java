package ewm.place.client;

import ewm.place.dto.PlaceDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "location-service", contextId = "placeClient", path = "/internal/places")
public interface PlaceClient {

    @GetMapping("/{placeId}")
    PlaceDto getPlace(@PathVariable("placeId") long placeId);
}