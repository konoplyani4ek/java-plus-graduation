package ewm.event.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "annotation", nullable = false, length = 2000)
    private String annotation;

    @Column(name = "description", nullable = false, length = 7000)
    private String description;

    // Категории — в main-service (доп. функциональность). Получаем через CategoryClient.
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Embedded
    private Location location;

    @Column(name = "paid", nullable = false)
    @Builder.Default
    private boolean paid = false;

    @Column(name = "participant_limit", nullable = false)
    private int participantLimit;

    @Column(name = "request_moderation", nullable = false)
    @Builder.Default
    private boolean requestModeration = true;

    @Column(name = "state", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EventState state = EventState.PENDING;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    // Пользователи — в user-service. Получаем через UserClient.
    @Column(name = "initiator_id", nullable = false)
    private Long initiatorId;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    // Места — в main-service (доп. функциональность). Получаем через PlaceClient.
    @Column(name = "place_id")
    private Long placeId;
}