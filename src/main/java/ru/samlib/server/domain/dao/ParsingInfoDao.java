package ru.samlib.server.domain.dao;


import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.samlib.server.domain.entity.ParsingInfo;
import ru.samlib.server.domain.entity.Work;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

@Transactional
@Repository
public interface ParsingInfoDao extends CrudRepository<ParsingInfo, Long> {

    ParsingInfo findFirstByOrderByLogDateDesc();

}
