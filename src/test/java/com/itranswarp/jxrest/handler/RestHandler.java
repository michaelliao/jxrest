package com.itranswarp.jxrest.handler;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.itranswarp.jsonstream.annotation.Format;
import com.itranswarp.jsonstream.annotation.JsonIgnore;
import com.itranswarp.jsonstream.annotation.MaxLength;
import com.itranswarp.jsonstream.annotation.MinLength;
import com.itranswarp.jsonstream.annotation.Required;
import com.itranswarp.jsonstream.format.Email;
import com.itranswarp.jsonstream.format.NonBlank;
import com.itranswarp.jxrest.ApiException;
import com.itranswarp.jxrest.GET;
import com.itranswarp.jxrest.POST;
import com.itranswarp.jxrest.PUT;
import com.itranswarp.jxrest.Path;

/**
 * For test.
 * 
 * @author Michael Liao
 */
public class RestHandler {

    long nextId = 0;
    Map<String, User> users = new ConcurrentHashMap<String, User>();

    String nextId() {
        nextId ++;
        return "u-" + nextId;
    }

    @GET
    @Path("/users/:id")
    public User createUser(String id) {
        User user = users.get(id);
        if (user == null) {
            throw new ApiException("404");
        }
        return user;
    }

    @PUT
    @Path("/users")
    public User createUser(User user) {
        String id = nextId();
        user.id = id;
        users.put(id, user);
        return user;
    }

    @POST
    @Path("/users/:id")
    public User updateUser(String id, User user) {
        User exist = users.get(id);
        if (exist == null) {
            throw new ApiException("entity:notfound", "User");
        }
        exist.name = user.name;
        exist.email = user.email;
        exist.password = user.password;
        return exist;
    }

    @POST
    @Path("/users/:userId/courses")
    public Course createCourse(String userId, Course course, HttpServletRequest request, HttpServletResponse response) throws Exception {
        User exist = users.get(userId);
        if (exist == null) {
            throw new ApiException("404");
        }
        course.id = "c-0001";
        course.userId = userId;
        // save course here...
        return course;
    }

}

abstract class Entity {

    long created_at;
    long version;

    public long getCreated_at() {
        return created_at;
    }

    @JsonIgnore
    public void setCreated_at(long created_at) {
        this.created_at = created_at;
    }

    public long getVersion() {
        return version;
    }

    @JsonIgnore
    public void setVersion(long version) {
        this.version = version;
    }

}

class User extends Entity {

    String id;
    String email;
    String password;
    String name;

    public String getId() {
        return id;
    }

    @JsonIgnore
    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    @Format(Email.class)
    public void setEmail(String email) {
        this.email = email;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }

    @MinLength(6)
    @MaxLength(20)
    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    @MinLength(1)
    @MaxLength(20)
    public void setName(String name) {
        this.name = name;
    }

}

class Course extends Entity {

    String id;

    String userId;

    @Required
    @MinLength(1)
    @MaxLength(20)
    @Format(NonBlank.class)
    String name;

    @Required
    LocalDate startDate;

    @Required
    LocalDate endDate;

}
