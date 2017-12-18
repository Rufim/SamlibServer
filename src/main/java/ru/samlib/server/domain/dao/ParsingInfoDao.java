package ru.samlib.server.domain.dao;


import org.springframework.data.repository.CrudRepository;
import ru.samlib.server.domain.entity.ParsingInfo;
import ru.samlib.server.domain.entity.Work;

import javax.transaction.Transactional;

@Transactional
public interface ParsingInfoDao extends CrudRepository<ParsingInfo, Long> {


    ParsingInfo findFirstByParsedTrueOrderByLogDateDesc();
}
