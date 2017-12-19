package ru.samlib.server.parser;

public enum Command {
    EDT,  //редактирование атрибутов
    TXT,  //textedit DEL
    NEW,  // new
    DEL,  // del
    RPL,  // поверх старого
    REN,  // переименование файла, вскобках новоеимя REN(444-2)
    UNK   // операция неопределилась

}
