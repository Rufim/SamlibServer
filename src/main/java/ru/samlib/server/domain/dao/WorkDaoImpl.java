package ru.samlib.server.domain.dao;

import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import ru.samlib.server.domain.entity.Genre;
import ru.samlib.server.domain.entity.Type;
import ru.samlib.server.domain.entity.Work;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

public class WorkDaoImpl implements WorkDaoCustom {

    @PersistenceContext
    private EntityManager em;

    private JpaEntityInformation<Work, ?> entityInformation;

    @PostConstruct
    public void postConstruct() {
        this.entityInformation = JpaEntityInformationSupport.getEntityInformation(Work.class, em);
    }

    @Override
    public List<Work> searchWorks(String query, Type type, Genre genre, Integer offset, Integer limit) {
        return null;
    }
}
