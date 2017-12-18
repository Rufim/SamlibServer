package ru.samlib.server.domain.dao;


import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.samlib.server.domain.entity.Author;
import ru.samlib.server.domain.entity.Work;

import javax.transaction.Transactional;
import java.util.Date;

@Transactional
@Repository
public interface AuthorDao extends CrudRepository<Author, String> {

}
