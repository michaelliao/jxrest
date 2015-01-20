package com.itranswarp.jxrest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * RestContext holds HttpServletRequest and HttpServletResponse object in thread local.
 * 
 * @author Michael Liao
 */
public class RestContext {

    private static ThreadLocal<RestContext> threadLocal = new ThreadLocal<RestContext>();
    private final HttpServletRequest req;
    private final HttpServletResponse resp;

    private RestContext(HttpServletRequest req, HttpServletResponse resp) {
        this.req = req;
        this.resp = resp;
    }

    public static RestContext getRestContext() {
        return threadLocal.get();
    }

    static void initRestContext(HttpServletRequest req, HttpServletResponse resp) {
        threadLocal.set(new RestContext(req, resp));
    }

    static void destroyRestContext() {
        threadLocal.set(null);
    }

    public HttpServletRequest getHttpServletRequest() {
        return req;
    }

    public HttpServletResponse getHttpServletResponse() {
        return resp;
    }

    public HttpSession getHttpSession() {
        return req.getSession();
    }

}
