package ru.samlib.server.domain.dao;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import ru.samlib.server.domain.entity.Genre;
import ru.samlib.server.domain.entity.Type;
import ru.samlib.server.domain.entity.Work;
import ru.samlib.server.util.TextUtils;

import javax.annotation.PostConstruct;
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
        sequence.append("select distinct w from Work as w inner join fetch w.genres as g");
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
        sequence.append(" ORDER BY w.updateDate DESC, w.activityCounter DESC");
        TypedQuery<Work> typedQuery = em.createQuery(sequence.toString(), Work.class);
        if(TextUtils.notEmpty(query)) typedQuery.setParameter("query", "%" + query + "%");
        if(type != null) typedQuery.setParameter("type", type);
        if(genre != null) typedQuery.setParameter("genre", genre);
        if(offset != null && offset >= 0) typedQuery.setFirstResult(offset);
        if(limit != null && limit >= 0) typedQuery.setMaxResults(limit);
        return typedQuery.getResultList();
    }
}
