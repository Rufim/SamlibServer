package ru.samlib.server.domain.dao;

import org.springframework.transaction.annotation.Transactional;
import ru.samlib.server.domain.entity.*;
import ru.samlib.server.util.Log;
import ru.samlib.server.util.TextUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.*;

public class WorkDaoImpl implements WorkDaoCustom {

    @PersistenceContext
    private EntityManager em;

    //private JpaEntityInformation<Work, ?> entityInformation;

    //@PostConstruct
    //public void postConstruct() {
    //    this.entityInformation = JpaEntityInformationSupport.getEntityInformation(Work.class, em);
    //}

    @Transactional
    @Override
    public int updateStat(Map<String, Integer> stats, String authorLink) {
        StringBuilder sequence = new StringBuilder();
        try {
            for (Map.Entry<String, Integer> stat : stats.entrySet()) {
                String link = stat.getKey();
                if (link.equals("./")) {
                    sequence.append("UPDATE author SET views = ");
                    sequence.append(stat.getValue());
                    sequence.append(" WHERE link = '");
                    sequence.append(authorLink);
                    sequence.append("'; \n");
                } else if (link.endsWith(".shtml") && !link.equals("about.html")) {
                    sequence.append("UPDATE work SET views = ");
                    sequence.append(stat.getValue());
                    sequence.append(" WHERE link = '");
                    sequence.append(authorLink + link.substring(0, link.lastIndexOf(".shtml")));
                    sequence.append("'; \n");
                }
            }
            if (!em.isJoinedToTransaction()) {
                em.joinTransaction();
            }
            return em.createNativeQuery(sequence.toString()).executeUpdate();
        } catch (Throwable ex) {
            Log.e("SQL_ERROR:", "error in " + sequence, ex);
        }
        return 0;
    }

    @Transactional
    @Override
    public int deleteNotIn(Collection<String> links, String authorLink) {
        StringBuilder sequenceGenres = new StringBuilder();
        StringBuilder sequenceWorks = new StringBuilder();
        try {
            sequenceGenres.append("DELETE FROM genres WHERE work_link like '");
            sequenceGenres.append(authorLink);
            sequenceGenres.append("%' and work_link not in (");
            sequenceWorks.append("DELETE FROM work WHERE link like '");
            sequenceWorks.append(authorLink);
            sequenceWorks.append("%' and link not in (");
            boolean first = true;
            for (String link : links) {
                if (!first) {
                    sequenceGenres.append(",");
                    sequenceWorks.append(",");
                }
                sequenceGenres.append("'");
                sequenceGenres.append(link);
                sequenceGenres.append("'");
                sequenceWorks.append("'");
                sequenceWorks.append(link);
                sequenceWorks.append("'");
                first = false;
            }
            sequenceGenres.append(");");
            sequenceWorks.append(");");
            if (!em.isJoinedToTransaction()) {
                em.joinTransaction();
            }
            return em.createNativeQuery(sequenceGenres.toString()).executeUpdate() + em.createNativeQuery(sequenceWorks.toString()).executeUpdate();
        } catch (Throwable ex) {
            Log.e("SQL_ERROR:", "error in " + sequenceGenres + " or " + sequenceWorks, ex);
        }
        return 0;
    }

    @Override
    public List<Work> searchWorksByActivityNative(String query, Type type, Genre genre, Integer offset, Integer limit) {
        StringBuilder sequence = new StringBuilder();
        try {
            sequence.append("SELECT ");
            sequence.append("work.link, work.title,work.annotation, work.work_author_name, ");
            sequence.append(genre != null ? "g.genre, " : "");
            sequence.append("work.activity_index, ");
            sequence.append("work.update_date ");
            sequence.append("FROM work ");
            sequence.append(genre != null ? "LEFT JOIN genres g ON public.work.link = g.work_link " : "");
            StringBuilder where = new StringBuilder();
            if (TextUtils.notEmpty(query)) {
                where.append("(lower(work.title) like lower(:query) or lower(work.work_author_name) like lower(:query))");
            }
            if (genre != null) {
                if (where.length() > 0) where.append(" and ");
                where.append("g.genre = :genre");
            }
            if (type != null) {
                if (where.length() > 0) where.append(" and ");
                where.append("work.type = :type");
            }
            if (where.length() > 0) {
                sequence.append(" where ");
                sequence.append(where);
            }
            sequence.append(" ORDER BY work.activity_index DESC, work.update_date DESC");
            if (limit != null && limit >= 0) {
                sequence.append(" LIMIT ");
                sequence.append(limit);
            }
            if (offset != null && offset >= 0) {
                sequence.append(" OFFSET ");
                sequence.append(offset);
            }
            Query nativeQuery = em.createNativeQuery(sequence.toString());
            if (TextUtils.notEmpty(query)) nativeQuery.setParameter("query", "%" + query + "%");
            if (type != null) nativeQuery.setParameter("type", type.name());
            if (genre != null) nativeQuery.setParameter("genre", genre.name());
            List res = nativeQuery.getResultList();
            ArrayList<Work> works = new ArrayList<>();
            for (Object re : res) {
                works.add(parseRow((Object[]) re));
            }
            return works;
        } catch (Throwable ex) {
            Log.e("SQL_ERROR:", "error in " + sequence, ex);
        }
        return new ArrayList<>();
    }

    private Work parseRow(Object[] row) {
        Work work = new Work(row[0].toString());
        work.setTitle(row[1].toString());
        work.setAnnotation(row[2].toString());
        work.setWorkAuthorName(row[3].toString());
        return work;
    }

    @Override
    public List<Work> searchWorksByActivity(String query, Type type, Genre genre, Integer offset, Integer limit) {
        StringBuilder sequence = new StringBuilder();
        sequence.append("select distinct w from Work as w join fetch w.genres as g");
        StringBuilder where = new StringBuilder();
        StringBuilder page = new StringBuilder();
        if (TextUtils.notEmpty(query)) {
            where.append("(lower(w.title) like lower(:query) or lower(w.workAuthorName) like lower(:query))");
        }
        if (genre != null) {
            if (where.length() > 0) where.append(" and ");
            where.append("g = :genre");
        }
        if (type != null) {
            if (where.length() > 0) where.append(" and ");
            where.append("w.type = :type");
        }
        if (where.length() > 0) {
            sequence.append(" where ");
            sequence.append(where);
        }
        sequence.append(" ORDER BY w.activityIndex DESC");
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
        if(TextUtils.isEmpty(origin.getFullName())){
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
