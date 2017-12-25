package ru.samlib.server.domain.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.samlib.server.domain.entity.Author;
import ru.samlib.server.domain.entity.Category;
import ru.samlib.server.domain.entity.Work;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

public class AuthorDaoImpl implements AuthorDaoCustom {

    @Autowired
    AuthorDao authorDao;
    @Autowired
    CategoryDao categoryDao;

    @PersistenceContext
    private EntityManager em;


    @Override
    @Transactional
    public void changeAuthorLink(Author oldAuthor, String newLink) {
        Author newAuthor = authorDao.findAuthorFetchWorks(oldAuthor.getLink());
        newAuthor.setCategories(categoryDao.findAllByAuthor(newAuthor));
        authorDao.delete(oldAuthor);
        authorDao.flush();
        newAuthor.setLink(newLink);
        for (Work work : newAuthor.getWorks()) {
            work.setNewAuthorLink(newAuthor.getLink());
        }
        for (Category category : newAuthor.getCategories()) {
            category.getId().setAuthorLink(newAuthor.getLink());
        }
        em.persist(newAuthor);
        for (Category category : newAuthor.getCategories()) {
            em.persist(category);
        }
        for (Work work : newAuthor.getWorks()) {
            em.persist(work);
        }
    }
}
