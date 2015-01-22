package com.itranswarp.jxrest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.itranswarp.jsonstream.JsonBuilder;
import com.itranswarp.jsonstream.JsonWriter;

/**
 * RestApiHandler serves JSON-REST API request, and send JSON-Response.
 * 
 * @author Michael Liao
 */
public class RestApiHandler {

    Log log = LogFactory.getLog(getClass());

    Routes routes = new Routes();
    JsonBuilder jsonBuilder = new JsonBuilder();

    public void setJsonBuilder(JsonBuilder jsonBuilder) {
        this.jsonBuilder = jsonBuilder;
    }

    public void setHandlers(List<String> names) {
        for (String name : names) {
            findHandlers(name);
        }
    }

    protected void findHandlers(String name) {
        for (Class<?> clazz : new ClassFinder().findClasses(name)) {
            addHandler(clazz);
        }
    }

    protected void addHandler(Class<?> clazz) {
        try {
            routes.addHandler(clazz.newInstance());
        }
        catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    protected void processApi(HttpServletRequest req, HttpServletResponse resp, String method, String path) throws IOException {
        switch (method) {
        case "GET":
            processApiRequest(req, resp, "GET", path);
            break;
        case "POST":
            processApiRequest(req, resp, "POST", path);
            break;
        case "PUT":
            processApiRequest(req, resp, "PUT", path);
            break;
        case "DELETE":
            processApiRequest(req, resp, "DELETE", path);
            break;
        default:
            processBadRequest(req, resp, method, path);
            break;
        }
    }

    protected Object parseBeanFromJson(Class<?> type, HttpServletRequest req) throws IOException {
        String encoding = req.getCharacterEncoding();
        if (encoding == null) {
            encoding = "UTF-8";
        }
        Reader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(req.getInputStream(), encoding));
            return jsonBuilder.createReader(reader).parse(type);
        }
        finally {
            if (reader != null) {
                reader.close();
            }
        }    
    }

    protected void processBadRequest(HttpServletRequest req, HttpServletResponse resp, String method, String path) throws IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
    }

    protected boolean checkContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        String ct = contentType.toLowerCase();
        if (ct.equals("application/json")) {
            return true;
        }
        if (ct.startsWith("application/json")) {
            char ch = ct.charAt(16);
            return ch == ' ' || ch == ';';
        }
        return false;
    }

    void processApiRequest(HttpServletRequest req, HttpServletResponse resp, String method, String path) throws IOException {
        // check content type:
        if (!"GET".equals(method) && (req.getContentLength()!=0 && !checkContentType(req.getContentType()))) {
            log.debug("415 UNSUPPORTED MEDIA TYPE: not a json request.");
            resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Request must be application/json.");
            return;
        }
        RestContext.initRestContext(req, resp);
        try {
            JsonCallback jsonCallback = null;
            if (!"GET".equals(method)) {
                jsonCallback = (Class<?> type) -> {
                    return parseBeanFromJson(type, req);
                };
            }
            Object ret = this.routes.call(method, path, jsonCallback, req, resp);
            if (ret instanceof Void) {
                return;
            }
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("application/json");
            Writer writer = resp.getWriter();
            JsonWriter jsonWriter = this.jsonBuilder.createWriter(writer);
            jsonWriter.write(ret);
            writer.flush();
        }
        catch (ApiNotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
        catch (Exception e) {
            log.error("Process API failed.", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        finally {
            RestContext.destroyRestContext();
        }
    }
}
