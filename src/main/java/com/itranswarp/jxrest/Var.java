package com.itranswarp.jxrest;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class Var {

    static final int PATH_VAR = 0;
    static final int JSON_VAR = 1;
    static final int QUERY_VAR = 2;
    static final int REQUEST_VAR = 3;
    static final int RESPONSE_VAR = 4;

    final int varType;
    final Class<?> argType;
    final String name;
    final int index;

    Var(int varType, Class<?> argType, String name, int index) {
        this.varType = varType;
        this.argType = argType;
        this.name = name;
        this.index = index;
    }

    static Var createPathVar(Class<?> argType, String name, int index) {
        return new Var(PATH_VAR, argType, name, index);
    }

    static Var createJsonVar(Class<?> type, String name, int index) {
        return new Var(JSON_VAR, type, name, index);
    }

    static Var createRequestVar(String name, int index) {
        return new Var(REQUEST_VAR, HttpServletRequest.class, name, index);
    }

    static Var createQueryVar(String name, int index) {
        return new Var(QUERY_VAR, Map.class, name, index);
    }

    static Var createResponseVar(String name, int index) {
        return new Var(RESPONSE_VAR, HttpServletResponse.class, name, index);
    }

}
