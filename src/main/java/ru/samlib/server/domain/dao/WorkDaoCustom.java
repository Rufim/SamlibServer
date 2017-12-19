package ru.samlib.server.domain.dao;

import org.springframework.data.domain.Pageable;
import ru.samlib.server.domain.entity.Genre;
import ru.samlib.server.domain.entity.Type;
import ru.samlib.server.domain.entity.Work;

import java.util.List;

public interface WorkDaoCustom {
    
    List<Work> searchWorks(String query, Type type, Genre genre, Integer offset, Integer limit);

}
