package com.itranswarp.jxrest.handler;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.itranswarp.jsonstream.JsonBuilder;

public class JettyAppTest {

    static final int PORT = 8123;
    static final String URL = "http://localhost:" + PORT;
    static Server server = null;

    @BeforeClass
    public static void startJetty() throws Exception {
        String url = new JettyAppTest().getClass().getClassLoader().getResource(new JettyAppTest().getClass().getPackage().getName().replace('.', '/')).toString();
        if (!url.startsWith("file:")) {
            throw new RuntimeException("Cannot run Jetty test in jar/war.");
        }
        String path = url.substring(5);
        System.out.println("Init webapp at " + path + "...");
        // init servlet context:
        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setWar(path);
        // init server:
        server = new Server(PORT);
        server.setHandler(webapp);
        server.start();
    }

    @AfterClass
    public static void shutdownJetty() throws Exception {
        server.stop();
        server.destroy();
    }

    String inputStreamToString(InputStream input) throws Exception {
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[128];
        Reader reader = new InputStreamReader(input, "UTF-8");
        for (;;) {
            int n = reader.read(buffer);
            if (n==(-1)) {
                break;
            }
            sb.append(buffer, 0, n);
        }
        return sb.toString();
    }

    @Test
    public void testUsingJetty() throws Exception {
        HttpClient client = HttpClients.createDefault();
        // get:
        HttpGet get = new HttpGet(URL + "/api/users");
        HttpResponse getResp = client.execute(get);
        assertEquals(200, getResp.getStatusLine().getStatusCode());
        assertTrue(getResp.getEntity().getContentType().getValue().startsWith("application/json"));
        assertEquals("{\"users\":[]}", inputStreamToString(getResp.getEntity().getContent()));
        // put:
        HttpPut put = new HttpPut(URL + "/api/users");
        put.setEntity(new StringEntity("{\"email\":\"abc@xyz.com\",\"password\":\"hello123\",\"name\":\"Michael\"}", ContentType.create("application/json; charset=UTF-8")));
        HttpResponse putResp = client.execute(put);
        assertEquals(200, putResp.getStatusLine().getStatusCode());
        assertTrue(putResp.getEntity().getContentType().getValue().startsWith("application/json"));
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) new JsonBuilder().createReader(putResp.getEntity().getContent()).parse();
        assertEquals(1L, map.get("id"));
        assertEquals("Michael", map.get("name"));
        assertNull(map.get("password"));
        // post:
        HttpPost post = new HttpPost(URL + "/api/users/1");
        post.setEntity(new StringEntity("{\"email\":\"updated@xyz.com\",\"password\":\"hello456\",\"name\":\"Bob\"}", ContentType.create("application/json; charset=UTF-8")));
        HttpResponse postResp = client.execute(post);
        assertEquals(200, postResp.getStatusLine().getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> map2 = (Map<String, Object>) new JsonBuilder().createReader(postResp.getEntity().getContent()).parse();
        assertEquals(1L, map2.get("id"));
        assertEquals("Bob", map2.get("name"));
        assertEquals("updated@xyz.com", map2.get("email"));
        assertNull(map2.get("password"));
        // delete:
        HttpDelete delete = new HttpDelete(URL + "/api/users/1");
        delete.addHeader("Content-Length", "0");
        HttpResponse deleteResp = client.execute(delete);
        assertEquals(200, deleteResp.getStatusLine().getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> map3 = (Map<String, Object>) new JsonBuilder().createReader(deleteResp.getEntity().getContent()).parse();
        assertEquals(1L, map3.get("id"));
        assertEquals(true, map3.get("deleted"));
    }
}
