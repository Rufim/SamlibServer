package ru.samlib.server.domain.dao;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.samlib.server.domain.entity.Author;

import javax.transaction.Transactional;
import java.util.List;

@Transactional
public interface AuthorDao extends JpaRepository<Author, String> {

    Author findFirstByMonthUpdateFiredFalseOrderByLastUpdateDateDesc();
}
