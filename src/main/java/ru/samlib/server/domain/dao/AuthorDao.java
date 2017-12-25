package ru.samlib.server.domain.dao;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.samlib.server.domain.entity.Author;

import javax.transaction.Transactional;
import java.util.List;

@Transactional
public interface AuthorDao extends JpaRepository<Author, String>, AuthorDaoCustom {

    Author findFirstByMonthUpdateFiredFalseAndDeletedFalseOrderByLastUpdateDateDesc();
    
    @Query("select a from Author as a left join fetch a.works as w where a.link = ?1")
    Author findAuthorFetchWorks(String link);
}
