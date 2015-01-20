package com.itranswarp.jxrest;

import static org.junit.Assert.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

public class RouteTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testRoute() {
        Pattern p = Route.compile("/test/:groupId/:userId").pattern;
        assertTrue(p.matcher("/test/123/456").matches());
        assertTrue(p.matcher("/test/A-001/B-007").matches());
        // not match:
        assertFalse(p.matcher("/test/A-007/B-001/").matches());
        assertFalse(p.matcher("/test//B-007").matches());
        assertFalse(p.matcher("/test/A-007/").matches());
        assertFalse(p.matcher("/test/A/B/C").matches());
        // test capture:
        Matcher m = p.matcher("/test/A-00123/B-00456");
        assertTrue(m.matches());
        assertEquals("A-00123", m.group("groupId"));
        assertEquals("B-00456", m.group("userId"));
    }

    @Test
    public void testRouteWithUnderscore() {
        Pattern p = Route.compile("/test_api/:groupId/:userId").pattern;
        assertTrue(p.matcher("/test_api/123_456/ABC_def").matches());
        assertTrue(p.matcher("/test_api/A_001/B_007").matches());
        // not match:
        assertFalse(p.matcher("/testapi/A/B/").matches());
        assertFalse(p.matcher("/test_api/_/").matches());
        assertFalse(p.matcher("/test_api/A//").matches());
        assertFalse(p.matcher("/test_api/A/B/C").matches());
        // test capture:
        Matcher m = p.matcher("/test_api/123_456/ABC_def");
        assertTrue(m.matches());
        assertEquals("123_456", m.group("groupId"));
        assertEquals("ABC_def", m.group("userId"));
    }

    @Test
    public void testRouteWithWarning() {
        Pattern p = Route.compile(":groupId/:userId").pattern;
        assertTrue(p.matcher("123/456").matches());
        assertTrue(p.matcher("A-001/B-007").matches());
        // not match:
        assertFalse(p.matcher("A/B/").matches());
        assertFalse(p.matcher("/B").matches());
        assertFalse(p.matcher("A/").matches());
        assertFalse(p.matcher("A/B/C").matches());
        // test capture:
        Matcher m = p.matcher("A-00123/B-00456");
        assertTrue(m.matches());
        assertEquals("A-00123", m.group("groupId"));
        assertEquals("B-00456", m.group("userId"));
    }

    @Test
    public void testRouteWithWarning2() {
        Pattern p = Route.compile("/test/:groupId:userId").pattern;
        assertTrue(p.matcher("/test/123456").matches());
        assertTrue(p.matcher("/test/AB").matches());
        // not match:
        assertFalse(p.matcher("/Test/A/B").matches());
        assertFalse(p.matcher("/test/A/B/").matches());
        assertFalse(p.matcher("/test/A").matches());
        assertFalse(p.matcher("/test/AB/").matches());
        assertFalse(p.matcher("/test/AB/C").matches());
        // test capture:
        Matcher m = p.matcher("/test/ABCDEF");
        assertTrue(m.matches());
        assertEquals("ABCDE", m.group("groupId"));
        assertEquals("F", m.group("userId"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRouteCannotCompile() {
        Route.compile("/test/:/");
    }

}
