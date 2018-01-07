package ru.samlib.server.domain.dao;

import org.springframework.transaction.annotation.Transactional;
import ru.samlib.server.domain.entity.*;
import ru.samlib.server.util.Log;
import ru.samlib.server.util.TextUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.math.BigDecimal;
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
    public int updateStat(Map<String, String> stats, String authorLink) {
        StringBuilder sequenceWork = new StringBuilder();
        StringBuilder sequenceAuthor = new StringBuilder();
        try {
            String title = null;
            String about = null;
            String views = null;
            for (Map.Entry<String, String> stat : stats.entrySet()) {
                String link = stat.getKey();
                if (link.equals("./")) {
                    views = stat.getValue();
                } else if (link.endsWith(".shtml") && !link.equals("about.html")) {
                    sequenceWork.append("UPDATE work SET views = ");
                    sequenceWork.append(stat.getValue());
                    sequenceWork.append(" WHERE link = '");
                    sequenceWork.append(authorLink + link.substring(0, link.lastIndexOf(".shtml")));
                    sequenceWork.append("'; \n");
                } else if (link.equals("title")) {
                    title = stat.getValue();
                } else if (link.equals("about")) {
                    about = stat.getValue();
                }
            }
            sequenceAuthor.append("UPDATE author SET");
            if (about != null && title != null) {
                sequenceAuthor.append(" full_name=:title");
                sequenceAuthor.append(", about=:about");
            }
            if (views != null) {
                if (about != null && title != null) {
                    sequenceAuthor.append(",");
                }
                sequenceAuthor.append(" views=:views");
            }
            if ((about != null && title != null) || views != null) {
                sequenceAuthor.append(",");
            }
            sequenceAuthor.append(" month_update_fired = true");
            sequenceAuthor.append(" WHERE link = '");
            sequenceAuthor.append(authorLink);
            sequenceAuthor.append("'; \n");
            Query authorQuery = em.createNativeQuery(sequenceAuthor.toString());
            if (about != null && title != null) {
                authorQuery.setParameter("title", title);
                authorQuery.setParameter("about", about);
            }
            if (views != null) {
                authorQuery.setParameter("views", Integer.valueOf(views));
            }
            if (!em.isJoinedToTransaction()) {
                em.joinTransaction();
            }
            return em.createNativeQuery(sequenceWork.toString()).executeUpdate() + authorQuery.executeUpdate();
        } catch (Throwable ex) {
            Log.e("SQL_ERROR:", "error in " + sequenceWork + "\n " + sequenceAuthor, ex);
            return -1;
        }
    }

    @Transactional
    @Override
    public int deleteNotIn(Collection<String> links, String authorLink) {
        StringBuilder sequenceGenres = new StringBuilder();
        StringBuilder sequenceWorks = new StringBuilder();
        try {
            sequenceGenres.append("DELETE FROM genres WHERE work_link like '");
            sequenceGenres.append(authorLink);
            sequenceGenres.append("%'");
            sequenceWorks.append("DELETE FROM work WHERE link like '");
            sequenceWorks.append(authorLink);
            sequenceWorks.append("%'");
            if (links.size() > 0) {
                sequenceGenres.append(" and work_link not in (");
                sequenceWorks.append(" and link not in (");
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
            } else {
                sequenceGenres.append(";");
            }
            if (!em.isJoinedToTransaction()) {
                em.joinTransaction();
            }
            return em.createNativeQuery(sequenceGenres.toString()).executeUpdate() + em.createNativeQuery(sequenceWorks.toString()).executeUpdate();
        } catch (Throwable ex) {
            Log.e("SQL_ERROR:", "error in " + sequenceGenres + " or " + sequenceWorks, ex);
            return -1;
        }
    }

    @Override
    public List<Work> searchWorksNative(String query, Type type, Genre genre, SortWorksBy searchBy, Integer size, Integer offset, Integer limit) {
        StringBuilder sequence = new StringBuilder();
        if (searchBy == null) {
            searchBy = SortWorksBy.ACTIVITY;
        }
        try {
            sequence.append("SELECT ");
            sequence.append("work.link, work.title,work.annotation, work.work_author_name, work.size, work.update_date, work.rate, work.votes, work.views ");
            sequence.append(", g.genre, work.type ");
            switch (searchBy) {
                case ACTIVITY:
                    sequence.append(", work.activity_index ");
                    break;
                case RATING:
                    sequence.append(", (work.votes/GREATEST(10 - work.rate, 0.1)) as r_index ");
                    break;
            }
            sequence.append("FROM work ");
            sequence.append("LEFT JOIN genres g ON public.work.link = g.work_link ");
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
            if (size != null && size > 0) {
                if (where.length() > 0) where.append(" and ");
                where.append("work.size >= :size");
            }
            switch (searchBy) {
                case RATING:
                    if (where.length() > 0) where.append(" and ");
                    where.append(" work.rate IS NOT NULL and work.votes IS NOT NULL ");
                    break;
            }
            if (where.length() > 0) {
                sequence.append(" where ");
                sequence.append(where);
            }
            switch (searchBy) {
                case ACTIVITY:
                    sequence.append(" ORDER BY work.activity_index DESC NULLS LAST, work.update_date DESC NULLS LAST");
                    break;
                case RATING:
                    sequence.append(" ORDER BY r_index DESC NULLS LAST");
                    break;
                case VIEWS:
                    sequence.append(" ORDER BY work.views DESC NULLS LAST");
                    break;
            }
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
            if (size != null && size > 0) nativeQuery.setParameter("size", size);
            List res = nativeQuery.getResultList();
            ArrayList<Work> works = new ArrayList<>();
            for (Object re : res) {
                Work work = parseRow((Object[]) re);
                if (work != null) {
                    works.add(work);
                }
            }
            return works;
        } catch (Throwable ex) {
            Log.e("SQL_ERROR:", "error in " + sequence, ex);
        }
        return new ArrayList<>();
    }

    private Work parseRow(Object[] row) {
        if (row[0] != null) {
            try {
                Work work = new Work(row[0].toString());
                if (row[1] != null) work.setTitle(row[1].toString());
                if (row[2] != null) {
                    String annot = row[2].toString();
                    if (annot != null && annot.endsWith("|")) {
                        annot = annot.substring(0, annot.lastIndexOf("|"));
                    }
                    work.setAnnotation(annot);
                }
                if (row[3] != null) work.setWorkAuthorName(row[3].toString());
                if (row[4] != null) work.setSize((Integer) row[4]);
                if (row[5] != null) work.setUpdateDate((Date) row[5]);
                if (row[6] != null) work.setRate((BigDecimal) row[6]);
                if (row[7] != null) work.setVotes((Integer) row[7]);
                if (row[8] != null) work.setViews((Integer) row[8]);
                try {
                    if (row[9] != null) work.addGenre(Genre.valueOf(row[9].toString()));
                    if (row[10] != null) work.setType(Type.valueOf(row[10].toString()));
                } catch (Throwable e) {
                }
                return work;
            } catch (Throwable e) {
            }
        }
        return null;
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
        if (TextUtils.notEmpty(query)) typedQuery.setParameter("query", "%" + query + "%");
        if (type != null) typedQuery.setParameter("type", type);
        if (genre != null) typedQuery.setParameter("genre", genre);
        if (offset != null && offset >= 0) typedQuery.setFirstResult(offset);
        if (limit != null && limit >= 0) typedQuery.setMaxResults(limit);
        return typedQuery.getResultList();
    }

    @Override
    @Transactional
    public Work saveWork(Work newWork, boolean update) {
        if (!em.isJoinedToTransaction()) {
            em.joinTransaction();
        }
        if (!update) {
            Category category = em.find(Category.class, new CategoryId(newWork.getAuthor().getLink(), newWork.getType().getTitle()));
            Author author = null;
            if (category != null) {
                // em.merge(newWork.getCategory()); // другой инфы о категории, кроме СategoryId, у нас нет -> мержить нечего
                newWork.setCategory(category);
                newWork.setAuthor(merge(category.getAuthor(), newWork.getAuthor()));
                author = category.getAuthor();
            } else {
                author = em.find(Author.class, newWork.getAuthor().getLink());
                if (author != null) {
                    newWork.setAuthor(merge(author, newWork.getAuthor()));
                    newWork.getCategory().setAuthor(newWork.getAuthor());
                }
            }
            if (author == null) em.persist(newWork.getAuthor());
            if (category == null) em.persist(newWork.getCategory());
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
        if (TextUtils.isEmpty(origin.getFullName())) {
            merge = true;
            origin.setFullName(newData.getFullName());
        }
        if (newData.getLastUpdateDate() != null && !newData.getLastUpdateDate().equals(origin.getLastUpdateDate())) {
            merge = true;
            origin.setLastUpdateDate(newData.getLastUpdateDate());
        }
        if (merge) {
            return em.merge(origin);
        }
        return origin;
    }
}
