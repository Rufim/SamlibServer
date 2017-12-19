package ru.samlib.server.domain.dao;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.samlib.server.domain.entity.Link;

import javax.transaction.Transactional;

@Transactional
public interface LinkDao extends JpaRepository<Link, Long> {


}
