package com.example.userlogin.service;

import javax.websocket.Session;

public interface WebSocketServer {
    void onOpen(Session session, Long userId);
    void onClose(Long userId);
    void onError(Session session, Throwable error);
}
