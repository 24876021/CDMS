package com.example.userlogin.controller;

import cn.hutool.core.map.MapUtil;
import com.example.userlogin.constant.RsaProperties;
import com.example.userlogin.model.*;
import com.example.userlogin.handler.password.PasswordEncoder;
import com.example.userlogin.service.*;
import com.example.userlogin.service.Impl.WebSocketServerImpl;
import com.example.userlogin.user.UserDetailServiceImpl;
import com.example.userlogin.utils.RSAUtils;
import com.example.userlogin.utils.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 用户控制器
 * @author shuangtian
 *
 * login 和 logout 由框架管理
 */
@RestController
@Api(tags = "用户管理相关接口")
@RequestMapping("/sysUser")
public class UserController {


    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    RoleService roleService;
    @Autowired
    private WsPushController wsPushController;
    @Autowired
    private RedisUtil redisUtil;

    @PostMapping("/register")
    @ApiOperation("用户注册")
    public Result register(@RequestBody SysUser sysUser){
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        try {
            //查询当前账号是否存在
            SysUser sysUser1 = sysUserService.getByAccount(sysUser.getAccount());
            if (sysUser1 !=null){
                return Result.fail("当前账号已经存在，请更换账号");
            }
            //RSA解密
            String pwd = RSAUtils.decryptByPrivate(sysUser.getPassword(), RsaProperties.privateKey);
            //BCrypt加密
            String encodePwd = passwordEncoder.encode(pwd);
            sysUser.setPassword(encodePwd);
            sysUser.setStatus(true);
            sysUser.setDisable(true);
            //虽然数据表的字段设有默认值，但其实际是在该字段接收到的值为null使用
            //后端实体类中布尔值的变量若没接收到前端数据则会设置其默认值为false后传入对象，在插入数据表时与其映射的字段的默认值就不生效了
            //又因为spring scrutiny中若用户被锁定和禁用则无法使用任何功能，因此必须在注册时手动设置其默认值为true,前端后端皆可
            sysUserService.save(sysUser);

            // 获取新注册用户的ID
            Long userId = sysUser.getUserId();
            // 向user_role表插入用户ID和角色ID
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(3L);//插入默认角色
            userRoleService.save(userRole);

            //注册成功推送
            wsPushController.pushByRole(1L, "register:" + sysUser.getAccount());

            return Result.succ("注册成功！");

        }finally {
            lock.unlock();
        }

    }
    @GetMapping("/RoleAndAndAuthority")
    @ApiOperation("获取当前登录用户的账户名与角色信息与权限信息")
    public Result getUserRoleAndAuthority(@RequestParam(value = "userId") Long userId) {
        // 获取账户名
        String account = SecurityContextHolder.getContext().getAuthentication().getName();

        // 获取持有权限
        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        List<String> authorityNames = authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());

        // 获取用户的角色ID列表
        List<Long> roleIds = userRoleService.getRolesByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return Result.fail("用户没有角色");
        }
        // 获取角色信息
        List<String> roleNames = roleService.getUserRoleName(roleIds);
        if (roleNames == null || roleNames.isEmpty()) {
            return Result.fail("用户角色信息为空");
        }

        return Result.succ(
                MapUtil.builder()
                        .put("account", account)
                        .put("role", roleNames)
                        .put("authority", authorityNames)
                        .build()
        );
    }
    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:get')")
    @GetMapping("/roles")
    @ApiOperation("查询用户所有角色")
    public Result2 getRolesbyUserId(@RequestParam(value = "userId") Long userId){
        List<Long> RoleList = userRoleService.getRolesByUserId(userId);//查询所有对象实体并存入列表
        if (!RoleList.isEmpty()){
            return Result2.success(RoleList);
        }else {
            return Result2.error("查询用户所有角色失败!");
        }
    }
    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:set')")
    @PutMapping("/updateUserRole")
    @ApiOperation("更改用户角色")
    public Result2 updateUserRole(@RequestParam Long userId, @RequestParam List<Long> roleIds) {
        // 如果 roleIds 为空，删除 userId 对应的所有角色记录
        if (roleIds == null || roleIds.isEmpty()) {
            userRoleService.removeById(userId);

            // 推送刷新
            WebSocketServerImpl.sendToUser(userId, "refreshPermissions");

            return Result2.success("删除角色所有权限成功！");
        }

        // 获取当前 userId 对应的权限列表
        List<Long> currentRoleIds = userRoleService.getRolesByUserId(userId);

        // 删除多余的角色记录
        for (Long currentRoleId : currentRoleIds) {
            if (!roleIds.contains(currentRoleId)) {
                userRoleService.deleteByUserIdAndRoleId(userId, currentRoleId);
            }
        }

        // 添加没有的角色记录
        for (Long roleId : roleIds) {
            if (!currentRoleIds.contains(roleId)) {
                UserRole userRole = new UserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoleService.save(userRole);
            }
        }

        // 推送权限刷新
        WebSocketServerImpl.sendToUser(userId, "refreshPermissions");

        return Result2.success("更改用户角色成功！");
    }
    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:get')")
    @GetMapping("/user")
    @ApiOperation("根据 userId 或 name 查询指定用户")
    public Result2 getUserByIdOrName(@RequestParam(value = "userId", required = false) Integer userId,
                                     @RequestParam(value = "name", required = false) String name) {
        SysUser sysUser = null;
        List<SysUser> sysUserList =null;
        if (userId != null) {
            sysUser = sysUserService.getById(userId);
        } else if (name != null) {
            sysUserList = sysUserService.getByUsername(name);
        } else {
            return Result2.error("请提供 userId 或 name 参数");
        }

        if (sysUser == null && sysUserList ==null) {
            return Result2.error("未找到指定用户");
        }

        if (sysUser != null) {
            return Result2.success(sysUser);
        } else {
            return Result2.success(sysUserList);
        }
    }
    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:get')")
    @GetMapping("/AllUsers")
    @ApiOperation("查询所有用户")
    public Result2 getAllUsers(){
        List<SysUser> userList = sysUserService.list();//查询所有对象实体并存入列表
        if (!userList.isEmpty()){
            return Result2.success(userList);
        }else {
            return Result2.error("查询所有用户失败!");
        }
    }
    @PreAuthorize("hasAuthority('resource:all') || hasAuthority('resource:set')")
    @PutMapping("/updateStatusAndDisable")
    @ApiOperation("修改用户状态")
    public Result2 updateUserStatus(@RequestBody SysUser sysuser){

        String lockKey = UserDetailServiceImpl.LOCK_KEY + sysuser.getAccount();

        // 手动解锁：立即清除Redis并在数据库解锁
        if (sysuser.isStatus()) {
            redisUtil.del(lockKey);
        }

        // 手动锁定：在数据库锁定，用户在登录时数据库被锁住的话会被立马添加Redis锁
        if (sysUserService.updateById(sysuser)){
            return Result2.success("更新状态成功！");
        } else {
            return Result2.error("更新状态失败");
        }
    }
}
