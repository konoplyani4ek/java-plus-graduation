package ewm.category.server.controller;

import ewm.category.server.service.CategoryService;
import ewm.category.server.dto.CategoryDto;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("categories")
@AllArgsConstructor
public class PublicCategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public Collection<CategoryDto> getAll(@RequestParam(required = false, defaultValue = "0") Integer from,
                                          @RequestParam(required = false, defaultValue = "10") Integer size) {
        return categoryService.getAll(from, size);
    }

    @GetMapping("{catId}")
    public CategoryDto getById(@PathVariable("catId") Long categoryId) {
        return categoryService.getById(categoryId);
    }
}