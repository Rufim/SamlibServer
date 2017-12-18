package ru.samlib.server.parser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import ru.samlib.server.domain.dao.*;
import ru.samlib.server.domain.entity.*;

@Service
public class CommandExecutorService {

    @Autowired
    private AuthorDao authorDao;
    @Autowired
    private CategoryDao categoryDao;
    @Autowired
    private WorkDao workDao;


    public void executeCommand(DataCommand dataCommand) {
        String link = dataCommand.link;
        if(link.endsWith("about")) {
            Work newWork = new Work(dataCommand.link);
            Author newAuthor = newWork.getAuthor();
            newAuthor.setFullName(dataCommand.authorName);
            newAuthor.setAnnotation(dataCommand.annotation);
            authorDao.save(newAuthor);
        } else {
            Work oldWork = workDao.findOne(link);
            Work newWork = new Work(dataCommand.link);
            newWork.getAuthor().setFullName(dataCommand.authorName);
            Category category = new Category();
            category.setType(dataCommand.type);
            category.setId(new CategoryId(newWork.getAuthor().getLink(), category.getTitle()));
            category.setAuthor(newWork.getAuthor());
            newWork.setCategory(category);
            newWork.addGenre(dataCommand.genre);
            newWork.setType(dataCommand.type);
            newWork.setChangedDate(dataCommand.commandDate);
            newWork.setAnnotation(dataCommand.annotation);
            newWork.setCreateDate(dataCommand.createDate);
            newWork.setSize(dataCommand.size);
            newWork.setUpdateDate(dataCommand.createDate);
            switch (dataCommand.getCommand()) {
                case EDT:
                case RPL:
                case REN:
                case UNK:
                    if (oldWork != null) {
                        newWork.setChangedDate(oldWork.getChangedDate());
                    } else {
                        newWork.getAuthor().setLastUpdateDate(newWork.getUpdateDate());
                    }
                    authorDao.save(newWork.getAuthor());
                    categoryDao.save(newWork.getCategory());
                    workDao.save(newWork);
                    break;
                case NEW:
                case TXT:
                    newWork.getAuthor().setLastUpdateDate(newWork.getUpdateDate());
                    authorDao.save(newWork.getAuthor());
                    categoryDao.save(newWork.getCategory());
                    workDao.save(newWork);
                    break;
                case DEL:
                    if (oldWork != null) {
                        workDao.delete(oldWork);
                    }
                    break;
            }
        }
    }




}
