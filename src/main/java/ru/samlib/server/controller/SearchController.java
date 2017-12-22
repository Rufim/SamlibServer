package ru.samlib.server.controller;


import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import ru.samlib.server.domain.dao.WorkDao;
import ru.samlib.server.domain.entity.Genre;
import ru.samlib.server.domain.entity.Type;
import ru.samlib.server.domain.entity.Work;
import ru.samlib.server.util.Log;
import ru.samlib.server.util.TextUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Controller
public class SearchController {

    int pageSize = 10;

    @Autowired
    WorkDao workDao;

    //http://localhost:8080/search-works?page=1&query=%D0%AF%D1%81%D0%B8%D0%BD%D1%81%D0%BA%D0%B8%D0%B9
    @GetMapping("/search-works")
    @ResponseBody
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

    @GetMapping("/search")
    public String searchWorks(@RequestParam("query") String query, @RequestParam("genre") String genre, @RequestParam("type") String type, @RequestParam("page") Integer page, ModelMap modelMap) {
        modelMap.addAttribute("works", searchWorks(query, genre, type, page));
        modelMap.addAttribute("query", query);
        modelMap.addAttribute("genre", genre);
        modelMap.addAttribute("type", type);
        modelMap.addAttribute("page", page);
        modelMap.addAttribute("types", (List<Type>) Stream.of(Type.values()).filter(t -> !Type.OTHER.equals(t)).collect(Collectors.toList()));
        modelMap.addAttribute("genres", (List<Genre>) Arrays.asList(Genre.values()));
        return "search";
    }

}
