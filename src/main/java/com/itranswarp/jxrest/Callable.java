package com.itranswarp.jxrest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Callable object to hold method and argument informations.
 * 
 * @author Michael Liao
 */
class Callable {

    final boolean isStatic;
    final String path;
    final Route route;
    final Object handlerInstance;
    final Method handlerMethod;
    final Var[] vars;

    Callable(Object handlerInstance, Class<?> handlerClass, Method handlerMethod, String httpMethod, String path) {
        handlerMethod.setAccessible(true);
        this.handlerInstance = handlerInstance;
        this.handlerMethod = handlerMethod;
        this.path = path;
        this.isStatic = path.indexOf(":") == (-1);
        this.route = isStatic ? null : new Route(path);
        this.vars = createVars(handlerClass, handlerMethod, this.route, httpMethod);
    }

    Var[] createVars(Class<?> clazz, Method method, Route route, String httpMethod) {
        List<Var> vars = new ArrayList<Var>();
        Parameter[] ps = method.getParameters();
        boolean foundJson = false;
        for (int index = 0; index < ps.length; index ++) {
            Parameter p = ps[index];
            String varName = p.getName();
            Class<?> varType = p.getType();
            if (HttpServletRequest.class.equals(varType)) {
                vars.add(Var.createRequestVar(varName, index));
            }
            else if (HttpServletResponse.class.equals(varType)) {
                vars.add(Var.createResponseVar(varName, index));
            }
            else if (varName.equals("query") && isMapStringString(p)) {
                vars.add(Var.createQueryVar(varName, index));
            }
            else if (route != null && route.hasParameter(varName)) {
                if (! isValidPathVariableType(varType)) {
                    throw new IllegalArgumentException("Unsupported path variable \"" + varType.getName() + " " + varName + "\" in " + toHandlerString(clazz, method));
                }
                vars.add(Var.createPathVar(varType, varName, index));
            }
            else if (! "GET".equals(httpMethod)) {
                if (foundJson) {
                    throw new IllegalArgumentException("Duplicate json variable \"" + varType.getName() + " " + varName + "\" in " + toHandlerString(clazz, method));
                }
                vars.add(Var.createJsonVar(varType, varName, index));
                foundJson = true;
            }
            else {
                throw new IllegalArgumentException("Unknown parameter \"" + varType.getName() + " " + varName + "\" in " + toHandlerString(clazz, method));
            }
        }
        return vars.toArray(new Var[vars.size()]);
    }

    /**
     * Is parameter has type `Map<String, String>`?
     */
    boolean isMapStringString(Parameter p) {
        if ( ! Map.class.equals(p.getType())) {
            return false;
        }
        Type type = p.getParameterizedType();
        if ( ! (type instanceof ParameterizedType)) {
            return false;
        }
        ParameterizedType pt = (ParameterizedType) type;
        Type[] types = pt.getActualTypeArguments();
        if (types.length != 2) {
            return false;
        }
        return String.class.equals(types[0]) && String.class.equals(types[1]);
    }

    String toHandlerString(Class<?> clazz, Method method) {
        List<String> paramNames = new ArrayList<String>();
        for (Parameter param : method.getParameters()) {
            paramNames.add(param.getName());
        }
        return clazz.getName() + "." + method.getName() + "(" + String.join(", ", paramNames) + ")";
    }

    public Object call(Map<String, String> pathVars, JsonCallback jsonCallback, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // prepare arguments:
        Object[] args = new Object[this.vars.length];
        for (int i=0; i<args.length; i++) {
            Var var = this.vars[i];
            switch (var.varType) {
            case Var.PATH_VAR:
                args[i] = convertPathVariable(pathVars.get(var.name), var.argType);
                break;
            case Var.JSON_VAR:
                args[i] = jsonCallback.getJson(var.argType);
                break;
            case Var.QUERY_VAR:
                args[i] = createQuery(request);
                break;
            case Var.REQUEST_VAR:
                args[i] = request;
                break;
            case Var.RESPONSE_VAR:
                args[i] = response;
                break;
            default:
                throw new RuntimeException("Bad var type: " + var.varType);
            }
        }
        try {
            return this.handlerMethod.invoke(this.handlerInstance, args);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    Map<String, String> createQuery(HttpServletRequest request) {
        Map<String, String> map = new HashMap<String, String>();
        for (Enumeration<String> e = request.getParameterNames(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            map.put(key, request.getParameter(key));
        }
        return map;
    }

    interface Converter {
        Object convert(String str);
    }

    static final Map<String, Converter> CONVERTERS = initConverters();

    static Map<String, Converter> initConverters() {
        Map<String, Converter> converters = new HashMap<String, Converter>();
        // to byte:
        Converter byteConverter = (str) -> {
            return Byte.parseByte(str);
        };
        converters.put(byte.class.getName(), byteConverter);
        converters.put(Byte.class.getName(), byteConverter);
        // to short:
        Converter shortConverter = (str) -> {
            return Short.parseShort(str);
        };
        converters.put(short.class.getName(), shortConverter);
        converters.put(Short.class.getName(), shortConverter);
        // to int:
        Converter intConverter = (str) -> {
            return Integer.parseInt(str);
        };
        converters.put(int.class.getName(), intConverter);
        converters.put(Integer.class.getName(), intConverter);
        // to long:
        Converter longConverter = (str) -> {
            return Long.parseLong(str);
        };
        converters.put(long.class.getName(), longConverter);
        converters.put(Long.class.getName(), longConverter);
        // to float:
        Converter floatConverter = (str) -> {
            return Float.parseFloat(str);
        };
        converters.put(float.class.getName(), floatConverter);
        converters.put(Float.class.getName(), floatConverter);
        // to double:
        Converter doubleConverter = (str) -> {
            return Double.parseDouble(str);
        };
        converters.put(double.class.getName(), doubleConverter);
        converters.put(Double.class.getName(), doubleConverter);
        // to number:
        converters.put(Number.class.getName(), (str) -> {
            try {
                return Long.parseLong(str);
            }
            catch (NumberFormatException e) {
                return Double.parseDouble(str);
            }
        });
        // to string:
        converters.put(String.class.getName(), (str) -> {
            return str;
        });
        return converters;
    }

    boolean isValidPathVariableType(Class<?> clazz) {
        return CONVERTERS.containsKey(clazz.getName());
    }

    Object convertPathVariable(String str, Class<?> clazz) {
        return CONVERTERS.get(clazz.getName()).convert(str);
    }

}
