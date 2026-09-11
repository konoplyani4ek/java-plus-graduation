package ewm.category.server.mapper;

import ewm.category.server.model.Category;
import ewm.category.server.dto.CategoryDto;
import ewm.category.server.dto.NewCategoryDto;

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