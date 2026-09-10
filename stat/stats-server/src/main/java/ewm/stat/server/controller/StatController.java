package ewm.stat.server.controller;

import ewm.stat.dto.HitDto;
import ewm.stat.dto.StatDto;
import ewm.stat.server.service.StatService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@AllArgsConstructor
public class StatController {
    private final StatService statService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveHit(@Valid @RequestBody HitDto dto) {
        statService.saveHit(dto);
    }

    @GetMapping("/stats")
    public List<StatDto> getStats(@RequestParam
                                  @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                  LocalDateTime start,
                                  @RequestParam
                                  @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                  LocalDateTime end,
                                  @RequestParam(required = false, defaultValue = "false") boolean unique,
                                  @RequestParam(required = false) List<String> uris) {

        return statService.getStats(start, end, unique, uris);
    }
}
