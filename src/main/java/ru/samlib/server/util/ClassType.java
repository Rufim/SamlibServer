package ru.samlib.server.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by 0shad on 20.05.2017.
 */
public enum ClassType {

    ARRAYLIST(ArrayList.class),
    STRING(String.class),
    ARRAY(Object[].class),
    CHAR(Character.class),
    BYTE(Byte.class),
    BOOLEAN(Boolean.class),
    SHORT(Short.class),
    INTEGER(Integer.class),
    LONG(Long.class),
    FLOAT(Float.class),
    DOUBLE(Double.class),
    CHARSEQUENCE(CharSequence.class),
    SERIALIZABLE(Serializable.class),
    SET(Set.class),
    LIST(List.class),
    MAP(Map.class),
    ENUM(Enum.class),
    UNSUPPORTED(Void.class);

    private Class<?> clazz;
    private Class<?> primitive;

    private ClassType(Class<?> Clazz) {
        this.clazz = Clazz;
        if (Primitives.isWrapperType(Clazz)) {
            this.primitive = Primitives.unwrap(Clazz);
        }
    }

    public static ClassType cast(Class<?> cl) {
        if (null == cl) cl = Void.class;
        for (ClassType type : values()) {
            if (Primitives.isPrimitive(cl) && type.primitive == cl) {
                return type;
            } else if (type.clazz.isAssignableFrom(cl)) {
                return type;
            } else {
                for (Class<?> intClass : cl.getInterfaces()) {
                    if (type.clazz.isAssignableFrom(intClass)) {
                        return type;
                    }
                }
            }
        }
        return UNSUPPORTED;
    }

    public static ClassType cast(Object obj) {
        if (null == obj) return cast(Void.class);
        return cast(obj.getClass());
    }

}
