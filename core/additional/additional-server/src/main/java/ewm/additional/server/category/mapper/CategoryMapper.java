package ewm.additional.server.category.mapper;

import ewm.additional.server.category.model.Category;
import ewm.additional.server.dto.CategoryDto;
import ewm.additional.server.dto.NewCategoryDto;

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