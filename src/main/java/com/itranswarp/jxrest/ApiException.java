package com.itranswarp.jxrest;

/**
 * Root exception for REST API with error code, error data and detailed message.
 * 
 * @author Michael Liao
 */
public class ApiException extends RuntimeException {

    private String code;
    private String data;

    /**
     * Construct an ApiException using code, data and message.
     * 
     * @param code Error code as string.
     * @param data Error data as string.
     * @param message Error message as string.
     */
    public ApiException(String code, String data, String message) {
        super(message);
        this.code = code;
        this.data = data;
    }

    /**
     * Construct an ApiException using code and message.
     * 
     * @param code Error code as string.
     * @param message Error message as string.
     */
    public ApiException(String code, String message) {
        this(code, null, message);
    }

    /**
     * Construct an ApiException using code.
     * 
     * @param code Error code as string.
     */
    public ApiException(String code) {
        this(code, null, null);
    }

    /**
     * Get error code as string.
     * 
     * @return Error code as string.
     */
    public String getCode() {
        return this.code;
    }

    /**
     * Get error data as string.
     * 
     * @return Error data as string.
     */
    public String getData() {
        return this.data;
    }
}
