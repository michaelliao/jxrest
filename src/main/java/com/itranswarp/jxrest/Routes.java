package com.itranswarp.jxrest;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A collection of Route objects.
 * 
 * @author Michael Liao
 */
class Routes {

    Log log = LogFactory.getLog(getClass());

    final Map<String, Callable> staticGetCallables = new HashMap<String, Callable>();
    final Map<String, Callable> staticPostCallables = new HashMap<String, Callable>();
    final Map<String, Callable> staticPutCallables = new HashMap<String, Callable>();
    final Map<String, Callable> staticDeleteCallables = new HashMap<String, Callable>();

    final Map<String, Map<String, Callable>> staticMethods;

    final List<Callable> regexGetCallables = new ArrayList<Callable>();
    final List<Callable> regexPostCallables = new ArrayList<Callable>();
    final List<Callable> regexPutCallables = new ArrayList<Callable>();
    final List<Callable> regexDeleteCallables = new ArrayList<Callable>();

    final Map<String, List<Callable>> regexMethods;

    static List<Class<? extends Annotation>> HTTP_ANNOS = Arrays.asList(
            GET.class,
            POST.class,
            PUT.class,
            DELETE.class);

    Routes() {
        staticMethods = new HashMap<String, Map<String, Callable>>();
        staticMethods.put("GET", staticGetCallables);
        staticMethods.put("POST", staticPostCallables);
        staticMethods.put("PUT", staticPutCallables);
        staticMethods.put("DELETE", staticDeleteCallables);
        regexMethods = new HashMap<String, List<Callable>>();
        regexMethods.put("GET", regexGetCallables);
        regexMethods.put("POST", regexPostCallables);
        regexMethods.put("PUT", regexPutCallables);
        regexMethods.put("DELETE", regexDeleteCallables);
    }

    public void addHandler(Object handler) {
        Class<?> clazz = handler.getClass();
        for (Method m : Utils.getAllMethods(clazz)) {
            if (m.isAnnotationPresent(Path.class)) {
                Annotation httpMethod = null;
                for (Class<? extends Annotation> anno : HTTP_ANNOS) {
                    if (m.isAnnotationPresent(anno)) {
                        if (httpMethod == null) {
                            httpMethod = m.getAnnotation(anno);
                        }
                        else {
                            throw new IllegalArgumentException("Found more than one http method definition: @" + anno.getSimpleName() + ", @"+ httpMethod.getClass().getSimpleName() + " at method: " + clazz.getName() + "." + m.getName() + "()");
                        }
                    }
                }
                if (httpMethod == null) {
                    throw new IllegalArgumentException("Not found http method annotation at method: " + clazz.getName() + "." + m.getName() + "()");
                }
                if (Modifier.isStatic(m.getModifiers())) {
                    throw new IllegalArgumentException("Invalid static method: " + clazz.getName() + "." + m.getName() + "()");
                }
                if (Modifier.isAbstract(m.getModifiers())) {
                    throw new IllegalArgumentException("Invalid abstract method: " + clazz.getName() + "." + m.getName() + "()");
                }
                addHandler(handler, clazz, m, httpMethod.annotationType().getSimpleName(), m.getAnnotation(Path.class).value());
            }
            else {
                for (Class<? extends Annotation> anno : HTTP_ANNOS) {
                    if (m.isAnnotationPresent(anno)) {
                        throw new IllegalArgumentException("Annotation found but @Path is missing at method: " + clazz.getName() + "." + m.getName() + "()");
                    }
                }
            }
        }
    }

    void addHandler(Object handler, Class<?> clazz, Method method, String httpMethod, String path) {
        Callable callable = new Callable(handler, clazz, method, httpMethod, path);
        if (callable.isStatic) {
            Map<String, Callable> map = staticMethods.get(httpMethod);
            if (map.containsKey(path)) {
                throw new IllegalArgumentException("Duplicate handler for " + httpMethod + " " + path);
            }
            map.put(path, callable);
            log.info(httpMethod + ": " + path + ", handler: " + toHandlerString(clazz, method));
        }
        else {
            List<Callable> callables = this.regexMethods.get(httpMethod);
            // check if duplicate:
            for (Callable c : callables) {
                if (c.path.equals(path)) {
                    throw new IllegalArgumentException("Duplicate handler for " + httpMethod + " " + path);
                }
            }
            callables.add(callable);
            log.info(httpMethod + ": " + path + ", handler: " + toHandlerString(clazz, method));
        }
    }

    String toHandlerString(Class<?> clazz, Method method) {
        List<String> paramNames = new ArrayList<String>();
        for (Parameter param : method.getParameters()) {
            paramNames.add(param.getName());
        }
        return clazz.getName() + "." + method.getName() + "(" + String.join(", ", paramNames) + ")";
    }

    public Object call(String httpMethod, String path, JsonCallback jsonCallback, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // try find static handler:
        Callable sc = staticMethods.get(httpMethod).get(path);
        if (sc != null) {
            return sc.call(null, jsonCallback, request, response);
        }
        // try find regex handler:
        List<Callable> list = regexMethods.get(httpMethod);
        for (Callable c : list) {
            Map<String, String> pathValues = c.route.matches(path);
            if (pathValues != null) {
                return c.call(pathValues, jsonCallback, request, response);
            }
        }
        throw new ApiNotFoundException();
    }
}

interface JsonCallback {

    Object getJson(Class<?> type) throws IOException;

}
