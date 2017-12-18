package ru.samlib.server.domain.dao;


import org.springframework.data.repository.CrudRepository;
import ru.samlib.server.domain.entity.LogEvent;
import ru.samlib.server.domain.entity.Work;

import javax.transaction.Transactional;

@Transactional
public interface LogEventDao extends CrudRepository<LogEvent, Long> {


}
