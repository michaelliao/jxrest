package com.itranswarp.jxrest;

import java.io.IOException;
import java.util.stream.Stream;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A REST API filter to handle REST API calls.
 * 
 * @author Michael Liao
 */
public class RestApiServlet extends GenericServlet {

    final Log log = LogFactory.getLog(getClass());
    String urlPrefix = "";
    RestApiHandler handler = null;

    /**
     * Set RestApiHandler to handle REST API.
     * 
     * @param handler Instance of RestApiHandler.
     */
    public void setRestApiHandler(RestApiHandler handler) {
        this.handler = handler;
    }

    /**
     * Set URL prefix. e.g. "/api/v1".
     * 
     * @param prefix Prefix string, or null if no prefix.
     */
    public void setUrlPrefix(String prefix) {
        prefix = (prefix == null) ? "" : prefix.trim();
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        if (!prefix.isEmpty() && !prefix.startsWith("/")) {
            log.error("Invalid urlPrefix: must start with /, but actual is: " + prefix);
            throw new IllegalArgumentException("Invalid urlPrefix parameter: " + prefix);
        }
        log.info("Set urlPrefix of RestApiFilter to: " + prefix);
        this.urlPrefix = prefix;
    }

    public void init() throws ServletException {
        log.info("Init RestApiServlet...");
        if (this.handler == null) {
            this.handler = new RestApiHandler();
        }
        setUrlPrefix(getInitParameter("urlPrefix"));
        String handlers = getInitParameter("handlers");
        if (handlers != null) {
            Stream.of(handlers.split("\\,")).map((s) -> {
                return s.trim();
            }).filter((s) -> {
                return !s.isEmpty();
            }).forEach((s) -> {
                this.handler.findHandlers(s);
            });
        }
    }

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI();
        if (path.startsWith(this.urlPrefix)) {
            String apiUrl = path.substring(this.urlPrefix.length());
            if (apiUrl.startsWith("/")) {
                log.info("Process API request: " + apiUrl);
                this.handler.processApi(req, resp, req.getMethod(), apiUrl);
                return;
            }
        }
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
    }

    public void destroy() {
        log.info("Destroy RestApiServlet...");
        this.handler = null;
    }

}
