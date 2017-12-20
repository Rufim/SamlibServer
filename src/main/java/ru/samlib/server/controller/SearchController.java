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
import ru.samlib.server.util.Log;
import ru.samlib.server.util.TextUtils;

import java.util.Collection;

@RestController
public class SearchController {

    int pageSize = 10;

    @Autowired
    WorkDao workDao;

    //http://localhost:8080/search-works?page=1&query=%D0%AF%D1%81%D0%B8%D0%BD%D1%81%D0%BA%D0%B8%D0%B9
    @GetMapping("/search-works")
    public Collection<Work> searchWorks(@RequestParam("query") String query, @RequestParam("genre") String genre, @RequestParam("type") String type, @RequestParam("page") Integer page) {
        Log.i(SearchController.class, "q=" + query + " g=" + genre + " t=" + type + " p=" + page);
        String queryVal = query == null ? "" : query;
        Genre genreVal = Genre.parseGenre(genre);
        Type typeVal = TextUtils.isEmpty(type) ? null : Type.parseType(type);
        Integer pageVal = page == null ? 0 : page;
        if(pageVal < 0) pageVal = 0;
        pageVal *= pageSize;
        return workDao.searchWorksByActivity(queryVal, typeVal, genreVal, pageVal, pageVal + pageSize);
    }



}
