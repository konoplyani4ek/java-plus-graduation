package ewm.main.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompilationDto {
    @Valid
    private List<EventShortDto> events;

    @NotNull
    private Long id;

    @NotNull
    private Boolean pinned;

    @NotBlank
    @Size(max = 50, message = "The name must not exceed 50 characters")
    private String title;
}
