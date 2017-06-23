package cucumber.runtime.ate;

import java.lang.reflect.Type;

public interface TypeIntrospector {
    public Type[] getGenericTypes(Class<?> clazz) throws Exception;
}
