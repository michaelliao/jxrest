package com.itranswarp.jxrest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Route by regular express.
 * 
 * @author Michael Liao
 */
class Route {

    static final Log log = LogFactory.getLog(Route.class);
    static final Pattern RE_ROUTE_VAR = Pattern.compile("\\:([A-Za-z\\_][A-Za-z0-9]*)");

    final String[] parameters;
    final Pattern regexPath;

    Route(String path) {
        PatternAndNames pan = compile(path);
        this.regexPath = pan.pattern;
        this.parameters = pan.names;
    }

    static PatternAndNames compile(String path) {
        StringBuilder sb = new StringBuilder(path.length() + 32);
        sb.append("^");
        int start = 0;
        List<String> names = new ArrayList<String>();
        for (;;) {
            Matcher m = RE_ROUTE_VAR.matcher(path);
            boolean found = m.find(start);
            if (found) {
                if (start == m.start()) {
                    log.warn("URL pattern has possible error: \"" + path + "\", at " + start);
                }
                else {
                    appendStatic(sb, path.substring(start, m.start()));
                }
                String name = m.group(1);
                start = m.end();
                appendVar(sb, name);
                names.add(name);
            }
            else {
                appendStatic(sb, path.substring(start));
                break;
            }
        }
        if (names.isEmpty()) {
            throw new IllegalArgumentException("Cannot compile path to a valid regular expression: " + path);
        }
        sb.append("$");
        return new PatternAndNames(Pattern.compile(sb.toString()), names.toArray(new String[names.size()]));
    }

    static void appendVar(StringBuilder sb, String name) {
        sb.append("(?<").append(name).append(">[^\\/]+)");
    }

    static void appendStatic(StringBuilder sb, String s) {
        for (int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                sb.append(c);
            }
            else if (c >= 'a' && c <= 'z') {
                sb.append(c);
            }
            else if (c >= '0' && c <= '9') {
                sb.append(c);
            }
            else {
                sb.append('\\').append(c);
            }
        }
    }

    boolean hasParameter(String param) {
        for (String s : this.parameters) {
            if (s.equals(param)) {
                return true;
            }
        }
        return false;
    }

    Map<String, String> matches(String path) {
        Matcher m = regexPath.matcher(path);
        if (m.matches()) {
            Map<String, String> map = new HashMap<String, String>();
            for (String param : this.parameters) {
                map.put(param, m.group(param));
            }
            return map;
        }
        return null;
    }
}

class PatternAndNames {

    final Pattern pattern;
    final String[] names;

    PatternAndNames(Pattern pattern, String[] names) {
        this.pattern = pattern;
        this.names = names;
    }
}
