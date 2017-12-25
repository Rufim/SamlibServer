package ru.samlib.server.domain.dao;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.samlib.server.domain.entity.Author;
import ru.samlib.server.domain.entity.Category;
import ru.samlib.server.domain.entity.CategoryId;

import javax.transaction.Transactional;
import java.util.List;

@Transactional
public interface CategoryDao extends JpaRepository<Category, CategoryId> {

    List<Category> findAllByAuthor(Author author);
}
