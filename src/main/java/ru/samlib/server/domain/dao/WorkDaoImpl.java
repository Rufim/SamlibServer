package ru.samlib.server.domain.dao;

import org.springframework.transaction.annotation.Transactional;
import ru.samlib.server.domain.entity.*;
import ru.samlib.server.util.TextUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

public class WorkDaoImpl implements WorkDaoCustom {

    @PersistenceContext
    private EntityManager em;

    //private JpaEntityInformation<Work, ?> entityInformation;

    //@PostConstruct
    //public void postConstruct() {
    //    this.entityInformation = JpaEntityInformationSupport.getEntityInformation(Work.class, em);
    //}

    @Override
    public List<Work> searchWorksByActivity(String query, Type type, Genre genre, Integer offset, Integer limit) {
        StringBuilder sequence = new StringBuilder();
        sequence.append("select distinct w from Work as w join fetch w.genres as g");
        StringBuilder where = new StringBuilder();
        StringBuilder page = new StringBuilder();
        if (TextUtils.notEmpty(query)) {
            where.append("(lower(w.title) like lower(:query) or lower(w.author.fullName) like lower(:query))");
        }
        if (genre != null) {
            if (where.length() > 0) where.append(" and ");
            where.append("g = :genre");
        }
        if (type != null) {
            if (where.length() > 0) where.append(" and ");
            where.append("w.type = :type");
        }
        if (where.length() > 0) sequence.append(" where " + where);
        sequence.append(" ORDER BY w.updateDate DESC, w.activityIndex DESC");
        TypedQuery<Work> typedQuery = em.createQuery(sequence.toString(), Work.class);
        if(TextUtils.notEmpty(query)) typedQuery.setParameter("query", "%" + query + "%");
        if(type != null) typedQuery.setParameter("type", type);
        if(genre != null) typedQuery.setParameter("genre", genre);
        if(offset != null && offset >= 0) typedQuery.setFirstResult(offset);
        if(limit != null && limit >= 0) typedQuery.setMaxResults(limit);
        return typedQuery.getResultList();
    }

    @Override
    @Transactional
    public Work saveWork(Work newWork, boolean update) {
        if(!em.isJoinedToTransaction()) {
            em.joinTransaction();
        }
        if(!update) {
            Category category = em.find(Category.class, new CategoryId(newWork.getAuthor().getLink(), newWork.getType().getTitle()));
            Author author = null;
            if(category != null) {
                // em.merge(newWork.getCategory()); // другой инфы о категории, кроме СategoryId, у нас нет -> мержить нечего
                newWork.setCategory(category);
                newWork.setAuthor(merge(category.getAuthor(), newWork.getAuthor()));
                author = category.getAuthor();
            } else {
                author = em.find(Author.class, newWork.getAuthor().getLink());
                if(author != null) {
                    newWork.setAuthor(merge(author, newWork.getAuthor()));
                    newWork.getCategory().setAuthor(newWork.getAuthor());
                }
            }
            if(author == null) em.persist(newWork.getAuthor());
            if(category == null) em.persist(newWork.getCategory());
            em.persist(newWork);
            return newWork;
        } else {
            Author author = em.merge(newWork.getAuthor());
            newWork.setAuthor(author);
            newWork.getCategory().setAuthor(author);
            newWork.setCategory(em.merge(newWork.getCategory()));
            return em.merge(newWork);
        }

    }

    private Author merge(Author origin, Author newData) {
        boolean merge = false;
        if(TextUtils.notEmpty(newData.getFullName()) && !newData.getFullName().equals(origin.getFullName())){
            merge = true;
            origin.setFullName(newData.getFullName());
        }
        if (newData.getLastUpdateDate() != null && !newData.getLastUpdateDate().equals(origin.getLastUpdateDate())) {
            merge = true;
            origin.setLastUpdateDate(newData.getLastUpdateDate());
        }
        if(merge) {
            return em.merge(origin);
        }
        return origin;
    }
}
