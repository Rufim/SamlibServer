package ru.samlib.server.domain.dao;

import org.springframework.transaction.annotation.Transactional;
import ru.samlib.server.domain.entity.Genre;
import ru.samlib.server.domain.entity.Type;
import ru.samlib.server.domain.entity.Work;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface WorkDaoCustom {

    @Transactional
    int updateStat(Map<String, String> stats, String authorLink);

    @Transactional
    int deleteNotIn(Collection<String> links, String authorLink);

    List<Work> searchWorksByActivityNative(String query, Type type, Genre genre, Integer offset, Integer limit);

    List<Work> searchWorksByActivity(String query, Type type, Genre genre, Integer offset, Integer limit);

    @Transactional
    Work saveWork(Work newWork, boolean update);
}
