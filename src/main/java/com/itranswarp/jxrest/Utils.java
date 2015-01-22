package com.itranswarp.jxrest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Utils for internal use only.
 * 
 * @author Michael Liao
 */
class Utils {

    static List<Method> getAllMethods(Class<?> clazz) {
        List<Method> methods = new ArrayList<Method>();
        while (!clazz.equals(Object.class)) {
            for (Method m : clazz.getDeclaredMethods()) {
                methods.add(m);
            }
            clazz = clazz.getSuperclass();
        }
        return methods;
    }
}
