package ru.samlib.server.domain.dao;

import ru.samlib.server.domain.entity.Genre;
import ru.samlib.server.domain.entity.Type;
import ru.samlib.server.domain.entity.Work;

import java.util.List;

public interface WorkDaoCustom {
    
    List<Work> searchWorksByActivity(String query, Type type, Genre genre, Integer offset, Integer limit);

    Work saveWork(Work newWork, boolean update);
}
