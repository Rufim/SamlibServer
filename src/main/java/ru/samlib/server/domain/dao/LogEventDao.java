package ru.samlib.server.domain.dao;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.samlib.server.domain.entity.LogEvent;

import javax.transaction.Transactional;

@Transactional
public interface LogEventDao extends JpaRepository<LogEvent, Long> {


}
