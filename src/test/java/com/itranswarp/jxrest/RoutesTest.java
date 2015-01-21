package com.itranswarp.jxrest;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

public class RoutesTest {

    static final Map<String, String> QUERY;

    static {
        Map<String, String> query = new HashMap<String, String>();
        query.put("pageIndex", "1");
        query.put("pageSize", "100");
        QUERY = Collections.unmodifiableMap(query);
    }

    JsonCallback jsonCallback;
    HttpServletRequest request;
    HttpServletResponse response;

    @SuppressWarnings("unchecked")
    <T> T mock(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {
            clazz
        }, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals("getParameter")) {
                    return QUERY.get(args[0]);
                }
                if (method.getName().equals("getParameterNames")) {
                    return new Vector<String>(QUERY.keySet()).elements();
                }
                return null;
            }
        });
    }

    @Before
    public void setUp() throws Exception {
        jsonCallback = (t) -> {
            if (! t.isAssignableFrom(User.class)) {
                throw new RuntimeException("Bad json type.");
            }
            User u = new User();
            u.id = "u-001";
            u.name = "Michael";
            u.gender = 1;
            u.email = "rest@itranswarp.com";
            return u;
        };
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @Test
    public void testRoutes() throws Exception {
        Routes routes = new Routes();
        routes.addHandler(new TestHandler());
        // static get:
        assertEquals("static get", routes.call("GET", "/static/get", jsonCallback, request, response));
        // static post:
        assertEquals("static-post-Michael/rest@itranswarp.com-100", routes.call("POST", "/static/post", jsonCallback, request, response));
        // static put:
        assertEquals("static-put-Michael/rest@itranswarp.com", routes.call("PUT", "/static/put", jsonCallback, request, response));
        // static delete:
        assertEquals("static-delete-Michael/rest@itranswarp.com-1", routes.call("DELETE", "/static/delete", jsonCallback, request, response));
        // regex get:
        assertEquals("Hello, Michael!", routes.call("GET", "/get/Michael", jsonCallback, request, response));
        // regex post:
        assertEquals("Michael-123-Michael/rest@itranswarp.com", routes.call("POST", "/post/Michael/123", jsonCallback, request, response));
        // regex put:
        assertEquals("1234567890-100", routes.call("PUT", "/put/1234567890", jsonCallback, request, response));
        // regex delete:
        assertEquals("1234.5", routes.call("DELETE", "/delete/1234.5", jsonCallback, request, response));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRoutesNameNotMatched() {
        Routes routes = new Routes();
        routes.addHandler(new BadNameHandler());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRoutesBadStaticMethod() {
        Routes routes = new Routes();
        routes.addHandler(new BadStaticHandler());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRoutesBadAbstractMethod() {
        Routes routes = new Routes();
        routes.addHandler(new BadAbstractHandler());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRoutesBadPost() {
        Routes routes = new Routes();
        routes.addHandler(new BadPostHandler());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRoutesBadPut() {
        Routes routes = new Routes();
        routes.addHandler(new BadPutHandler());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRoutesBadDelete() {
        Routes routes = new Routes();
        routes.addHandler(new BadDeleteHandler());
    }

    @Test
    public void testProxy() throws Exception {
        Routes routes = new Routes();
        routes.addHandler(new SubHandler());
        assertEquals("Hello, Michael", routes.call("GET", "/hello/Michael", jsonCallback, request, response));
        assertEquals("Goodbye, Michael", routes.call("GET", "/goodbye/Michael", jsonCallback, request, response));
        // using proxy:
        Routes routes2 = new Routes();
        routes2.addHandler(new ProxyHandler());
        assertEquals("Hello, Michael!", routes2.call("GET", "/hello/Michael", jsonCallback, request, response));
        assertEquals("Goodbye, Michael!", routes2.call("GET", "/goodbye/Michael", jsonCallback, request, response));
    }
}

class TestHandler {

    @GET
    @Path("/get/:name")
    String regexGet(String name, HttpServletRequest req) {
        return "Hello, " + name + "!";
    }

    @POST
    @Path("/post/:name/:ver")
    String regexPost(HttpServletRequest resp, String name, int ver, User user) {
        return name + "-" + ver + "-" + user;
    }

    @PUT
    @Path("/put/:name")
    String regexPut(Number name, Map<String, String> query) {
        return name + "-" + query.get("pageSize");
    }

    @DELETE
    @Path("/delete/:name")
    String regexDelete(double name) {
        return new Double(name).toString();
    }

    @GET
    @Path("/static/get")
    String staticGet() {
        return "static get";
    }

    @POST
    @Path("/static/post")
    String staticPost(User json, Map<String, String> query) {
        return "static-post-" + json + "-" + query.get("pageSize");
    }

    @PUT
    @Path("/static/put")
    String staticPut(Object json) {
        return "static-put-" + json;
    }

    @DELETE
    @Path("/static/delete")
    String staticDelete(Map<String, String> query, User json) {
        return "static-delete-" + json + "-" + query.get("pageIndex");
    }

}

class BadNameHandler {

    @GET
    @Path("/hello/:name/:ver")
    String parameterNameNotFound(String name, String version) {
        return "Hello, " + name + "!";
    }
}

class BadGetHandler {

    @GET
    @Path("/hello/:name")
    String hello(String name, User bean) {
        return "Hello, " + name + "!";
    }
}

class BadPostHandler {

    @POST
    @Path("/hello/:name")
    String hello(String name, User bean, User user2) {
        return "Hello, " + name + "!";
    }
}

class BadPutHandler {

    @PUT
    @Path("/hello/:name")
    String hello(User bean, String name, User bean2) {
        return "Hello, " + name + "!";
    }
}

class BadDeleteHandler {

    @DELETE
    @Path("/hello/name")
    String hello(User bean, String name) {
        return "Hello, " + name + "!";
    }
}

class BadStaticHandler {
    @GET
    @Path("/static/get")
    static String get() {
        return "123";
    }
}

abstract class AbstractHandler {

    @GET
    @Path("/static/get")
    abstract String get();
}

class BadAbstractHandler extends AbstractHandler {
    String get() {
        return "123";
    }
}

interface IHandler {
    public String hello(String n);
}

class ParentHandler {
    @GET
    @Path("/goodbye/:name")
    public String goodbye(String name) {
        System.out.println("ParentHandler.goodbye()");
        return "Goodbye, " + name;
    }
}

class SubHandler extends ParentHandler implements IHandler {

    @GET
    @Path("/hello/:name")
    public String hello(String name) {
        System.out.println("SubHandler.hello()");
        return "Hello, " + name;
    }    
}

class ProxyHandler extends SubHandler {

    @Override
    public String hello(String name) {
        System.out.println("ProxyHandler.hello()");
        return super.hello(name) + "!";
    }

    @Override
    public String goodbye(String name) {
        System.out.println("ProxyHandler.goodbye()");
        return super.goodbye(name) + "!";
    }
    
}

class User {
    String id;
    String name;
    String email;
    int gender;

    public String toString() {
        return name + "/" + email;
    }
}
