package ru.samlib.server.controller;


import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.samlib.server.domain.dao.SortWorksBy;
import ru.samlib.server.domain.entity.Genre;
import ru.samlib.server.domain.entity.Type;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

@Controller
public class MainController {


    @GetMapping("/")
    public String index(ModelMap modelMap) {
        modelMap.addAttribute("types", (List<Type>) Stream.of(Type.values()).filter(t -> !Type.OTHER.equals(t)).collect(Collectors.toList()));
        modelMap.addAttribute("genres", (List<Genre>) Arrays.asList(Genre.values()));
        modelMap.addAttribute("sorts", (List<SortWorksBy>) Arrays.asList(SortWorksBy.values()));
        return "search";
    }

}
