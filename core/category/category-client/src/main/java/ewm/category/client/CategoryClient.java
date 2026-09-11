package ewm.category.client;

import ewm.category.dto.CategoryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "category-service", contextId = "categoryClient", path = "/internal/categories")
public interface CategoryClient {

    @GetMapping("/{categoryId}")
    CategoryDto getCategory(@PathVariable("categoryId") long categoryId);

    @GetMapping
    List<CategoryDto> getCategories(@RequestParam("ids") List<Long> ids);
}