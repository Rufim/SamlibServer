package ru.samlib.server.util;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.*;
import java.util.*;

import static ru.samlib.server.util.Primitives.checkArgument;

/**
 * Created by 0shad on 13.07.2015.
 */
public class ReflectionUtils {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface Value {
        String name();
    }

    public static <C> C cloneObject(C original) {
        try {
            C clone = (C) original.getClass().newInstance();
            for (Field field : getAllFieldsValues(original.getClass())) {
                field.setAccessible(true);
                if (field.get(original) == null || Modifier.isFinal(field.getModifiers())) {
                    continue;
                }
                if (field.getType().isPrimitive() || field.getType().equals(String.class)
                        || field.getType().getSuperclass().equals(Number.class)
                        || field.getType().equals(Boolean.class)) {
                    field.set(clone, field.get(original));
                } else {
                    Object childObj = field.get(original);
                    if (childObj == original) {
                        field.set(clone, clone);
                    } else {
                        field.set(clone, cloneObject(field.get(original)));
                    }
                }
            }
            return clone;
        } catch (Exception e) {
            return null;
        }
    }

    public static <S, T> T castObject(S source, Class<T> targetClass) throws IllegalAccessException, InstantiationException {
        T newInstance = targetClass.newInstance();
        for (Field field : source.getClass().getDeclaredFields()) {
            for (Field fieldTarget : targetClass.getDeclaredFields()) {
                if (isFieldsEqual(field, fieldTarget)) {
                    setField(getField(field, source), fieldTarget, newInstance);
                }
            }
        }
        return newInstance;
    }

    private static boolean isFieldsEqual(Field one, Field two) {
        return one.getName().equals(two.getName()) &&
                one.getType().equals(two);
    }

    public static Map<String, Object> getClassValues(Object obj) {
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        for (Field field : getAllFieldsValues(obj.getClass())) {
            Value annotation;
            // check if field has annotation
            if ((annotation = field.getAnnotation(Value.class)) != null) {
                String name = annotation.name();
                if (name == null) {
                    name = field.getName();
                }
                values.put(name, getField(field, obj));
            }
        }
        return values;
    }


    public static Map<String, Object> getClassFields(Object obj) {
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        for (Field field : getAllFieldsValues(obj.getClass())) {
            values.put(field.getName(), getField(field, obj));
        }
        return values;
    }

    public static Object getField(Field field, Object source) {
        boolean accessible = field.isAccessible();
        field.setAccessible(true);
        Object result = null;
        try {
            result = field.get(source);
        } catch (IllegalAccessException e) {
            // ignore
        }
        field.setAccessible(accessible);
        return result;
    }

    public static void setField(Object value, Field field, Object target) {
        boolean accessible = field.isAccessible();
        field.setAccessible(true);
        Object result = null;
        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            // ignore
        }
        field.setAccessible(accessible);
    }

    /**
     * Return a list of all fields (whatever access status, and on whatever
     * superclass they were defined) that can be found on this class.
     * This is like a union of {@link Class#getDeclaredFields()} which
     * ignores and super-classes, and {@link Class#getFields()} which ignored
     * non-public fields
     *
     * @param clazz The class to introspect
     * @return The complete list of fields
     */
    public static Field[] getAllFieldsValues(Class<?> clazz) {
        List<Class<?>> classes = getAllSuperclasses(clazz);
        classes.add(clazz);
        return getAllFields(classes);
    }


    /**
     * Return a list of all fields (whatever access status, and on whatever
     * superclass they were defined) that can be found on this class.
     * This is like a union of {@link Class#getDeclaredFields()} which
     * ignores and super-classes, and {@link Class#getFields()} which ignored
     * non-public fields
     *
     * @param clazz The class to introspect
     * @return The complete list of fields
     */
    public static Field[] getAllFields(Class<?> clazz) {
        List<Class<?>> classes = getAllSuperclasses(clazz);
        classes.add(clazz);
        return getAllFields(classes);
    }

    /**
     * As {@link #getAllFields(Class)} but acts on a list of {@link Class}s and
     * uses only {@link Class#getDeclaredFields()}.
     *
     * @param classes The list of classes to reflect on
     * @return The complete list of fields
     */
    private static Field[] getAllFields(List<Class<?>> classes) {
        Set<Field> fields = new HashSet<Field>();
        for (Class<?> clazz : classes) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        }
        return fields.toArray(new Field[fields.size()]);
    }

    /**
     * Return a List of super-classes for the given class.
     *
     * @param clazz the class to look up
     * @return the List of super-classes in order going up from this one
     */
    public static List<Class<?>> getAllSuperclasses(Class<?> clazz) {
        List<Class<?>> classes = new ArrayList<Class<?>>();

        Class<?> superclass = clazz.getSuperclass();
        while (superclass != null) {
            classes.add(superclass);
            superclass = superclass.getSuperclass();
        }

        return classes;
    }

    public static <T extends Object> T newInstance(Class<T> cl, Object... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<T> constructor = cl.getDeclaredConstructor(new Class[0]);
        boolean accessible = constructor.isAccessible();
        constructor.setAccessible(true);
        T t = constructor.newInstance(args);
        constructor.setAccessible(accessible);
        return t;
    }


    public static <T extends Object> void callMethod(T receiver, String methodName, Object... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        callMethodCast(receiver, null, methodName, args);
    }

    public static <T extends Object, Result> Result callMethodCast(T receiver, Class<Result> resultClass, String methodName, Object... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (receiver == null || methodName == null) {
            return null;
        }
        Class<?> cls = receiver.getClass();
        Method toInvoke = null;
        do {
            Method[] methods = cls.getDeclaredMethods();
            methodLoop:
            for (Method method : methods) {
                if (!methodName.equals(method.getName())) {
                    continue;
                }
                Class<?>[] paramTypes = method.getParameterTypes();
                if (args == null && paramTypes == null) {
                    toInvoke = method;
                    break;
                } else if (args == null || paramTypes == null
                        || paramTypes.length != args.length) {
                    continue;
                }

                for (int i = 0; i < args.length; ++i) {
                    if (!paramTypes[i].isAssignableFrom(args[i].getClass())) {
                        continue methodLoop;
                    }
                }
                toInvoke = method;
            }
        } while (toInvoke == null && (cls = cls.getSuperclass()) != null);
        Result t;
        if (toInvoke != null) {
            boolean accessible = toInvoke.isAccessible();
            toInvoke.setAccessible(true);
            if (resultClass != null) {
                t = resultClass.cast(toInvoke.invoke(receiver, args));
            } else {
                toInvoke.invoke(receiver, args);
                t = null;
            }
            toInvoke.setAccessible(accessible);
            return t;
        } else {
            throw new NoSuchMethodException("Method " + methodName + " not found");
        }
    }

    // Working whith Types

    public static Class<?> getRawType(Type type) {
        if (type instanceof Class<?>) {
            // type is a normal class.
            return (Class<?>) type;

        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            // I'm not exactly sure why getRawType() returns Type instead of Class.
            // Neal isn't either but suspects some pathological case related
            // to nested classes exists.
            Type rawType = parameterizedType.getRawType();
            checkArgument(rawType instanceof Class);
            return (Class<?>) rawType;

        } else if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            return Array.newInstance(getRawType(componentType), 0).getClass();

        } else if (type instanceof TypeVariable) {
            // we could use the variable's bounds, but that won't work if there are multiple.
            // having a raw type that's more general than necessary is okay
            return Object.class;

        } else if (type instanceof WildcardType) {
            return getRawType(((WildcardType) type).getUpperBounds()[0]);

        } else {
            String className = type == null ? "null" : type.getClass().getName();
            throw new IllegalArgumentException("Expected a Class, ParameterizedType, or "
                    + "GenericArrayType, but <" + type + "> is of type " + className);
        }
    }

    public static Type getParameterUpperBound(int index, ParameterizedType type) {
        Type[] types = type.getActualTypeArguments();
        if (types.length <= index) {
            throw new IllegalArgumentException(
                    "Expected at least " + index + " type argument(s) but got: " + Arrays.toString(types));
        }
        Type paramType = types[index];
        if (paramType instanceof WildcardType) {
            return ((WildcardType) paramType).getUpperBounds()[0];
        }
        return paramType;
    }

    /**
     * Для некоторого класса определяет каким классом был параметризован один из его предков с generic-параметрами.
     *
     * @param actualClass   анализируемый класс
     * @param genericClass  класс, для которого определяется значение параметра
     * @param parameterIndex номер параметра
     * @return        класс, являющийся параметром с индексом parameterIndex в genericClass
     */
    public static Class getGenericParameterClass(final Class actualClass, final Class genericClass, final int parameterIndex) {
        // Прекращаем работу если genericClass не является предком actualClass.
        if (!genericClass.isAssignableFrom(actualClass.getSuperclass())) {
            throw new IllegalArgumentException("Class " + genericClass.getName() + " is not a superclass of "
                    + actualClass.getName() + "build/intermediates/exploded-aar/io.fabric.sdk.android/fabric/1.3.10/res");
        }

        // Нам нужно найти класс, для которого непосредственным родителем будет genericClass.
        // Мы будем подниматься вверх по иерархии, пока не найдем интересующий нас класс.
        // В процессе поднятия мы будем сохранять в genericClasses все классы - они нам понадобятся при спуске вниз.

        // Проейденные классы - используются для спуска вниз.
        Stack<ParameterizedType> genericClasses = new Stack<ParameterizedType>();

        // clazz - текущий рассматриваемый класс
        Class clazz = actualClass;

        while (true) {
            Type genericSuperclass = clazz.getGenericSuperclass();
            boolean isParameterizedType = genericSuperclass instanceof ParameterizedType;
            if (isParameterizedType) {
                // Если предок - параметризованный класс, то запоминаем его - возможно он пригодится при спуске вниз.
                genericClasses.push((ParameterizedType) genericSuperclass);
            } else {
                // В иерархии встретился непараметризованный класс. Все ранее сохраненные параметризованные классы будут бесполезны.
                genericClasses.clear();
            }
            // Проверяем, дошли мы до нужного предка или нет.
            Type rawType = isParameterizedType ? ((ParameterizedType) genericSuperclass).getRawType() : genericSuperclass;
            if (!rawType.equals(genericClass)) {
                // genericClass не является непосредственным родителем для текущего класса.
                // Поднимаемся по иерархии дальше.
                clazz = clazz.getSuperclass();
            } else {
                // Мы поднялись до нужного класса. Останавливаемся.
                break;
            }
        }

        // Нужный класс найден. Теперь мы можем узнать, какими типами он параметризован.
        Type result = genericClasses.pop().getActualTypeArguments()[parameterIndex];

        while (result instanceof TypeVariable && !genericClasses.empty()) {
            // Похоже наш параметр задан где-то ниже по иерархии, спускаемся вниз.

            // Получаем индекс параметра в том классе, в котором он задан.
            int actualArgumentIndex = getParameterTypeDeclarationIndex((TypeVariable) result);
            // Берем соответствующий класс, содержащий метаинформацию о нашем параметре.
            ParameterizedType type = genericClasses.pop();
            // Получаем информацию о значении параметра.
            result = type.getActualTypeArguments()[actualArgumentIndex];
        }

        if (result instanceof TypeVariable) {
            // Мы спустились до самого низа, но даже там нужный параметр не имеет явного задания.
            // Следовательно из-за "Type erasure" узнать класс для параметра невозможно.
            throw new IllegalStateException("Unable to resolve type variable " + result + "build/intermediates/exploded-aar/io.fabric.sdk.android/fabric/1.3.10/res"
                    + " Try to replace instances of parametrized class with its non-parameterized subtype.");
        }

        if (result instanceof ParameterizedType) {
            // Сам параметр оказался параметризованным.
            // Отбросим информацию о его параметрах, она нам не нужна.
            result = ((ParameterizedType) result).getRawType();
        }

        if (result == null) {
            // Should never happen. :)
            throw new IllegalStateException("Unable to determine actual parameter type for "
                    + actualClass.getName() + "build/intermediates/exploded-aar/io.fabric.sdk.android/fabric/1.3.10/res");
        }

        if (!(result instanceof Class)) {
            // Похоже, что параметр - массив или что-то еще, что не является классом.
            throw new IllegalStateException("Actual parameter type for " + actualClass.getName() + " is not a Class.");
        }

        return (Class) result;
    }

    public static int getParameterTypeDeclarationIndex(final TypeVariable typeVariable) {
        GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration();

        // Ищем наш параметр среди всех параметров того класса, где определен нужный нам параметр.
        TypeVariable[] typeVariables = genericDeclaration.getTypeParameters();
        Integer actualArgumentIndex = null;
        for (int i = 0; i < typeVariables.length; i++) {
            if (typeVariables[i].equals(typeVariable)) {
                actualArgumentIndex = i;
                break;
            }
        }
        if (actualArgumentIndex != null) {
            return actualArgumentIndex;
        } else {
            throw new IllegalStateException("Argument " + typeVariable.toString() + " is not found in "
                    + genericDeclaration.toString() + "build/intermediates/exploded-aar/io.fabric.sdk.android/fabric/1.3.10/res");
        }
    }
}


