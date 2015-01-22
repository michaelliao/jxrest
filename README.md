### Overview

jxrest is a simple, lightweight framework to enable really fast development of JSON-based REST API.

Let's see how simple to write a REST API:

```
import com.itranswarp.jxrest.GET
import com.itranswarp.jxrest.Path

class MyRestApi {
    @GET
    @Path("/hello/:name")
    String hello(String name) {
        return "Hello, " + name + "!";
    }
}
```

That's all! jxrest convert your method to a REST API by simply add two annotations:

* @GET: Indicate this is a GET request;
* @Path: The API request path, variables can be included.

Using built-in RestApiFilter or RestApiServlet to deploy your API to a JavaEE-compatible server. 
Please check [documentation](http://jxrest.itranswarp.com) for more information.
