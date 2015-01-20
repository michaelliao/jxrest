package com.itranswarp.jxrest;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class App {

    public static void main(String[] args) throws Exception {
        int port = 8086;
        for (String arg : args) {
            if (arg.startsWith("port=")) {
                port = Integer.parseInt(arg.substring(5));
            }
        }
        // init servlet context:
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        // init server:
        Server server = new Server(port);
        server.setHandler(context);
        server.start();
        server.join();
    }

}
