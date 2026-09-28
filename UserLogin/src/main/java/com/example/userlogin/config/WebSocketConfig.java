package com.example.userlogin.config;

import com.example.userlogin.service.Impl.WebSocketServerImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
public class WebSocketConfig {
 @Bean
 public ServerEndpointExporter serverEndpointExporter() {
     return new ServerEndpointExporter();
 }
}