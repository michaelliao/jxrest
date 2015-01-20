package com.itranswarp.jxrest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark REST API path like "/api/users/:userId".
 * 
 * @author Michael Liao
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Path {

    /**
     * The value of the path. Path variables are allowed as :arg.
     * 
     * @return Path variable.
     */
	String value();

}
