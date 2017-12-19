package ru.samlib.server.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.samlib.server.domain.dao.WorkDao;
import ru.samlib.server.domain.entity.Genre;
import ru.samlib.server.domain.entity.Type;
import ru.samlib.server.domain.entity.Work;

import java.util.Collection;

@RestController
public class SearchController {

    int pageSize = 10;

    @Autowired
    WorkDao workDao;

    @GetMapping("/search-works")
    public Collection<Work> searchWorks(@RequestParam("query") String query, @RequestParam("genre") String genre, @RequestParam("type") String type, @RequestParam("page") Integer page) {
        String queryVal = query == null ? "" : query;
        Genre genreVal = Genre.parseGenre(genre);
        Type typeVal = Type.parseType(type);
        Integer pageVal = page == null ? 0 : page;
        if(pageVal < 0) pageVal = 0;
        pageVal *= pageSize;
        PageRequest pageRequest = new PageRequest(pageVal , pageVal + pageSize, Sort.Direction.DESC, "views");
        if(genreVal == null && typeVal == null) {
            return workDao.searchWorksSimple(queryVal, pageRequest);
        }
        if(typeVal == null) {
            return workDao.searchWorksSimple(queryVal, genreVal, pageRequest);
        }
        if(genreVal == null) {
            return workDao.searchWorksSimple(queryVal, typeVal, pageRequest);
        }
        return workDao.searchWorksSimple(queryVal, typeVal, genreVal, pageRequest);
    }



}
