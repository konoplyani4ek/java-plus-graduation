package ewm.main.category.controller;

import ewm.main.category.service.CategoryService;
import ewm.main.dto.CategoryDto;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("categories")
@AllArgsConstructor
public class PublicCategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public Collection<CategoryDto> getAll(@RequestParam(required = false,defaultValue = "0") Integer from,@RequestParam(required = false,defaultValue = "10") Integer size) {
        return categoryService.getAll(from,size);
    }

    @GetMapping("{catId}")
    public CategoryDto getById(@PathVariable("catId") Long categoryId) {
        return categoryService.getById(categoryId);
    }
}
