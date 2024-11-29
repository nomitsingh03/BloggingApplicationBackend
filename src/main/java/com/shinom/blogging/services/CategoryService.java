package com.shinom.blogging.services;

import java.util.List;

import com.shinom.blogging.dto.CategoryDto;

public interface CategoryService {

	CategoryDto creatCategory(CategoryDto categoryDto);
	
	CategoryDto updateCategory(CategoryDto categoryDto, Integer id);
	
	public void deleteCategory(Integer id);
	
	public CategoryDto getCategory(Integer id);
	
	public List<CategoryDto> getAllCategory();
	
}
