package ewm.compilation.server.controller;

import ewm.compilation.server.service.CompilationService;
import ewm.compilation.server.dto.CompilationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/compilations")
@RequiredArgsConstructor
public class PublicCompilationController {
    private final CompilationService compilationService;

    @GetMapping
    public List<CompilationDto> get(@RequestParam(required = false) Boolean pinned,
                                    @RequestParam(defaultValue = "0") Integer from,
                                    @RequestParam(defaultValue = "10") Integer size) {
        return compilationService.get(from, size, pinned);
    }

    @GetMapping("/{compId}")
    public CompilationDto getById(@PathVariable("compId") Long id) {
        return compilationService.get(id);
    }
}