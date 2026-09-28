package com.example.userlogin.service.Impl;

import com.example.userlogin.service.WebSocketServer;
import org.springframework.stereotype.Component;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint("/ws/{userId}")
public class WebSocketServerImpl implements WebSocketServer {

    public static final Map<Long, Session> SESSION_POOL = new ConcurrentHashMap<>();

    @OnOpen
    @Override
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        SESSION_POOL.put(userId, session);
        System.out.println("WebSocket 连接：userId=" + userId);
    }

    @OnClose
    @Override
    public void onClose(@PathParam("userId") Long userId) {
        SESSION_POOL.remove(userId);
        System.out.println("WebSocket 断开：userId=" + userId);
    }

    @OnError
    @Override
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    // 发送给单个用户
    public static void sendToUser(Long userId, String message) {
        Session session = SESSION_POOL.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 发送给所有用户
    public static void sendToAll(String message) {
        for (Session session : SESSION_POOL.values()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
