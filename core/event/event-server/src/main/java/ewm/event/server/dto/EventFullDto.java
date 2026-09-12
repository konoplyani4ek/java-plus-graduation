package ewm.event.server.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import ewm.category.dto.CategoryDto;
import ewm.place.dto.PlaceDto;
import ewm.user.dto.UserShortDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventFullDto {
    @NotBlank
    private String annotation;

    @Valid
    @NotNull
    private CategoryDto category;

    private Long confirmedRequests;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdOn;

    private String description;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    private Long id;

    @Valid
    @NotNull
    private UserShortDto initiator;

    @Valid
    @NotNull
    private LocationDto location;

    private PlaceDto place;

    @NotNull
    private Boolean paid;

    @Builder.Default
    private Integer participantLimit = 0;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedOn;

    @Builder.Default
    private Boolean requestModeration = true;

    private String state;

    @NotBlank
    private String title;

    private Long views;
}