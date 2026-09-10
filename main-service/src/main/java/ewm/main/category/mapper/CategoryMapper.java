package ewm.main.category.mapper;

import ewm.main.category.Category;
import ewm.main.dto.CategoryDto;
import ewm.main.dto.NewCategoryDto;

public class CategoryMapper {
    public static CategoryDto toDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    public static Category toEntity(NewCategoryDto dto) {
        return Category.builder()
                .name(dto.getName())
                .build();
    }
}
