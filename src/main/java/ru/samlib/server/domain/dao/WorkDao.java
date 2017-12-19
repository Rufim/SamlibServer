package ru.samlib.server.domain.dao;


import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.samlib.server.domain.entity.Genre;
import ru.samlib.server.domain.entity.Type;
import ru.samlib.server.domain.entity.Work;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.List;

@Transactional
public interface WorkDao extends JpaRepository<Work, String>, WorkDaoCustom {

    @Query("select distinct w from Work as w left outer join w.author as a inner join fetch w.genres as g where (w.title like %?1% or a.fullName like %?1%) and w.type = ?2 and g = ?3")
    List<Work> searchWorksSimple(String query, Type type, Genre genre, Pageable pageable);

    @Query("select distinct w from Work as w left outer join w.author as a inner join fetch w.genres as g where (w.title like %?1% or a.fullName like %?1%) and g = ?2")
    List<Work> searchWorksSimple(String query, Genre genre, Pageable pageable);

    @Query("select distinct w from Work as w left outer join w.author as a inner join fetch w.genres as g where (w.title like %?1% or a.fullName like %?1%) and w.type = ?2")
    List<Work> searchWorksSimple(String query, Type type, Pageable pageable);

    @Query("select distinct w from Work as w left outer join w.author as a inner join fetch w.genres as g where (w.title like %?1% or a.fullName like %?1%)")
    List<Work> searchWorksSimple(String query, Pageable pageable);
}
