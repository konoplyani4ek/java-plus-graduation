package ewm.main.compilation.controller;

import ewm.main.compilation.service.CompilationService;
import ewm.main.dto.CompilationDto;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "compilations")
@AllArgsConstructor
public class PublicCompilationController {
    private final CompilationService service;

    @GetMapping()
    public List<CompilationDto> get(@RequestParam(required = false, defaultValue = "0") Integer from,
                                    @RequestParam(required = false, defaultValue = "10") Integer size,
                                    @RequestParam(required = false) Boolean pinned
    ) {
        return service.get(from, size, pinned);
    }

    @GetMapping("{id}")
    public CompilationDto get(@PathVariable long id) {
        return service.get(id);
    }
}
