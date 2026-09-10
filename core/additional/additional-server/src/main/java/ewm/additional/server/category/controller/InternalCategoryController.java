package ewm.additional.server.category.controller;

import ewm.additional.server.category.model.Category;
import ewm.additional.server.category.repository.CategoryRepository;
import ewm.additional.server.exception.NotFoundException;
import ewm.category.dto.CategoryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/internal/categories")
@RequiredArgsConstructor
public class InternalCategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping("/{categoryId}")
    public CategoryDto getCategory(@PathVariable long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + categoryId + " не найдена"));
        return toDto(category);
    }

    @GetMapping
    public List<CategoryDto> getCategories(@RequestParam List<Long> ids) {
        return categoryRepository.findAllById(ids).stream()
                .map(this::toDto)
                .toList();
    }

    private CategoryDto toDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}