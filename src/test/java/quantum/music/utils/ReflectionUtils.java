package quantum.music.utils;

import java.lang.reflect.Field;

public class ReflectionUtils {

    public static void setValue(Object object, String fieldName, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }
}
