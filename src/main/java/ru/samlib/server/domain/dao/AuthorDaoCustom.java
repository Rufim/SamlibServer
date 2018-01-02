package ru.samlib.server.domain.dao;

import org.springframework.transaction.annotation.Transactional;
import ru.samlib.server.domain.entity.Author;

public interface AuthorDaoCustom {

    @Transactional
    void changeAuthorLink(Author oldAuthor, String newLink);

    @Transactional
    void restartCheckStat();
}
