package ewm.main.category.controller;

import ewm.main.category.service.CategoryService;
import ewm.main.dto.CategoryDto;
import ewm.main.dto.NewCategoryDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {
    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto add(@Valid @RequestBody NewCategoryDto dto) {
        return categoryService.add(dto);
    }

    @DeleteMapping("{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOne(@PathVariable("catId") Long categoryId) {
        categoryService.deleteOne(categoryId);
    }

    @PatchMapping("{catId}")
    public CategoryDto patchById(@PathVariable("catId") Long categoryId,@Valid @RequestBody NewCategoryDto dto) {
        return categoryService.patchById(categoryId,dto);
    }
}
