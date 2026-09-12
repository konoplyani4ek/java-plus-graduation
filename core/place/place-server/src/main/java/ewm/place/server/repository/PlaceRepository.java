package ewm.place.server.repository;

import ewm.place.server.model.Place;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {
}