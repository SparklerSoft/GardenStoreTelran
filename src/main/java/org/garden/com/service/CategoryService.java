package org.garden.com.service;

import org.garden.com.entity.Category;

import java.util.List;

public interface CategoryService {

    Category createCategory(Category category);

    List<Category> getAllCategories();

    Category editCategory(long id, Category category);

    Category deleteCategoryById(long id);

}
