package ewm.main.compilation.controller;

import ewm.main.compilation.service.CompilationService;
import ewm.main.dto.CompilationDto;
import ewm.main.dto.NewCompilationDto;
import ewm.main.dto.UpdateCompilationRequestDto;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/admin/compilations")
@AllArgsConstructor
public class AdminCompilationController {
    private final CompilationService service;

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto add(@Valid @RequestBody NewCompilationDto dto) {
        return service.add(dto);
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PatchMapping("{id}")
    public CompilationDto update(@PathVariable Long id, @Valid @RequestBody UpdateCompilationRequestDto dto) {
        return service.update(id, dto);
    }
}
