package ru.samlib.server.domain.dao;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.samlib.server.domain.entity.ParsingInfo;

import javax.transaction.Transactional;

@Transactional
public interface ParsingInfoDao extends JpaRepository<ParsingInfo, Long> {


    ParsingInfo findFirstByParsedTrueOrderByLogDateDesc();
}
