package com.arcadia.whiteRabbitService.web.context;

import org.springframework.stereotype.Component;

@Component
public class RequestContext {
    private String token;

    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
}