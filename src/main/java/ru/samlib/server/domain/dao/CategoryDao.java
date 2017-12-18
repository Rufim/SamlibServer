package ru.samlib.server.domain.dao;


import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.samlib.server.domain.entity.Category;
import ru.samlib.server.domain.entity.CategoryId;
import ru.samlib.server.domain.entity.Work;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface CategoryDao extends CrudRepository<Category, CategoryId> {


}
