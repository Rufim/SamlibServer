package ru.samlib.server.domain.dao;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.samlib.server.domain.entity.Category;
import ru.samlib.server.domain.entity.CategoryId;

import javax.transaction.Transactional;

@Transactional
public interface CategoryDao extends JpaRepository<Category, CategoryId> {


}
