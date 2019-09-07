package org.android.onviflibrary;

import java.util.HashMap;
import java.util.Map;

public class OnvifResponse {
    private OnvifRequest request;
    private boolean success;
    private int code;
    private String responseMessage;
    private String message;
    private String errorMessage;
    private Map<String, String> headers = new HashMap<>();

    public OnvifResponse putHeader(String key, String value){
        headers.put(key, value);
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String header(String key){
        return headers.get(key);
    }
    public Map<String, String> getHeaders(){
        return headers;
    }
    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public OnvifRequest getRequest() {
        return request;
    }

    public void setRequest(OnvifRequest request) {
        this.request = request;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
