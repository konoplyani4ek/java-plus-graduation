package ewm.main.category.service;

import ewm.main.category.Category;
import ewm.main.category.mapper.CategoryMapper;
import ewm.main.category.repository.CategoryRepository;
import ewm.main.dto.CategoryDto;
import ewm.main.dto.NewCategoryDto;
import ewm.main.event.repository.EventRepository;
import ewm.main.exception.ConflictException;
import ewm.main.exception.DataIntegrityViolationException;
import ewm.main.exception.NotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final EntityManager entityManager;
    private final EventRepository eventRepository;

    @Transactional
    public CategoryDto add(NewCategoryDto dto) {
        if (checkForNameCollisions(dto)) {
            throw new DataIntegrityViolationException("category already exists");
        }
        Category newCategory = categoryRepository.save(CategoryMapper.toEntity(dto));
        return CategoryMapper.toDto(newCategory);
    }

    @Transactional
    public CategoryDto patchById(Long categoryId, NewCategoryDto dto) {
        Category existingCategory = categoryRepository.findById(categoryId).orElseThrow(() -> new NotFoundException("no category found"));
        if (checkForNameAndIdCollisions(dto, categoryId)) {
            throw new DataIntegrityViolationException("category already exists");
        }

        if (!Objects.isNull(dto.getName())) {
            existingCategory.setName(dto.getName());
        }

        return CategoryMapper.toDto(existingCategory);
    }

    @Transactional
    public void deleteOne(Long categoryId) {
        Category existingCategory = categoryRepository.findById(categoryId).orElseThrow(() -> new NotFoundException("no category found"));

        if (eventRepository.existsByCategory_Id(categoryId)) {
            throw new ConflictException("Нельзя удалить категорию, к которой привязаны события");
        }

        categoryRepository.delete(existingCategory);
    }

    @Transactional
    public List<CategoryDto> getAll(Integer from, Integer size) {
        List<Category> categoryList = categoryRepository.findAllWithLimitOffset(from, size);

        return categoryList.stream().map(CategoryMapper::toDto).collect(Collectors.toList());
    }

    @Transactional
    public CategoryDto getById(Long categoryId) {
        Category existingCategory = categoryRepository.findById(categoryId).orElseThrow(() -> new NotFoundException("no category found"));

        return CategoryMapper.toDto(existingCategory);
    }

    private Boolean checkForNameCollisions(NewCategoryDto dto) {
        return categoryRepository.existsByName(dto.getName());
    }

    private boolean checkForNameAndIdCollisions(NewCategoryDto dto, Long id) {
        return categoryRepository.countByNameExcludingId(id, dto.getName()) > 0;
    }
}
