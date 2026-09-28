package com.example.userlogin.controller;

import com.example.userlogin.model.Result2;
import com.example.userlogin.service.Impl.WebSocketServerImpl;
import com.example.userlogin.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/ws")
public class WsPushController {

    @Autowired
    UserRoleService userRoleService;

    @GetMapping("/push/{userId}")
    public Result2 pushToUser(@PathVariable Long userId, String msg) {
        WebSocketServerImpl.sendToUser(userId, msg);
        return Result2.success("推送成功");
    }

    @GetMapping("/pushRole/{roleId}")
    public Result2 pushByRole(@PathVariable Long roleId, String msg) {
        List<Long> userIds = userRoleService.getUserIdsByRoleId(roleId);
        for (Long userId : userIds) {
            WebSocketServerImpl.sendToUser(userId, msg);
        }
        return Result2.success("按角色推送成功");
    }

    @GetMapping("/kickUser/{userId}")
    public Result2 kickUser(@PathVariable Long userId) {
        try {
            // 向前端发送固定指令：forceLogout
            WebSocketServerImpl.sendToUser(userId, "forceLogout");
            return Result2.success("已强制该用户下线");
        } catch (Exception e) {
            return Result2.error("用户不在线或推送失败");
        }
    }
}
