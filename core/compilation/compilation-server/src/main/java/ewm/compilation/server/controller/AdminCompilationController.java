package java.ewm.compilation.server.controller;

import ewm.compilation.server.service.CompilationService;
import ewm.compilation.server.dto.CompilationDto;
import ewm.compilation.server.dto.NewCompilationDto;
import ewm.compilation.server.dto.UpdateCompilationRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
public class AdminCompilationController {
    private final CompilationService compilationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto add(@Valid @RequestBody NewCompilationDto dto) {
        return compilationService.add(dto);
    }

    @DeleteMapping("/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("compId") Long id) {
        compilationService.delete(id);
    }

    @PatchMapping("/{compId}")
    public CompilationDto update(@PathVariable("compId") Long id, @Valid @RequestBody UpdateCompilationRequestDto dto) {
        return compilationService.update(id, dto);
    }
}