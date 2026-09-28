package com.example.userlogin.controller;

import com.example.userlogin.model.Authority;
import com.example.userlogin.model.Result2;
import com.example.userlogin.model.Role;
import com.example.userlogin.model.RoleAuthority;
import com.example.userlogin.service.AuthorityService;
import com.example.userlogin.service.Impl.WebSocketServerImpl;
import com.example.userlogin.service.RoleAuthorityService;
import com.example.userlogin.service.RoleService;
import com.example.userlogin.service.UserRoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Api(tags = "角色管理相关接口")
@RequestMapping("/role")
public class RoleController {
    @Autowired
    RoleService roleService;
    @Autowired
    AuthorityService authorityService;
    @Autowired
    RoleAuthorityService roleAuthorityService;
    @Autowired
    private UserRoleService userRoleService;

    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:get')")
    @GetMapping("/AllRoles")
    @ApiOperation("查询所有角色")
    public Result2 getAllUsers(){
        List<Role> roleList = roleService.list();//查询所有对象实体并存入列表
        if (!roleList.isEmpty()){
            return Result2.success(roleList);
        }else {
            return Result2.error("查询所有角色失败!");
        }
    }
    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:get')")
    @GetMapping("/AllAuthorities")
    @ApiOperation("查询所有权限")
    public Result2 getAllAuthorities(){
        List<Authority> AuthorityList = authorityService.list();//查询所有对象实体并存入列表
        if (!AuthorityList.isEmpty()){
            return Result2.success(AuthorityList);
        }else {
            return Result2.error("查询所有权限失败!");
        }
    }
    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:get')")
    @GetMapping("/Authorities")
    @ApiOperation("查询角色所有权限")
    public Result2 getAuthoritiesbyRoleId(@RequestParam(value = "roleId") Long roleId){
        List<Long> AuthorityList = roleAuthorityService.getAuthoritysByRoleIds(roleId);//查询所有对象实体并存入列表
        if (!AuthorityList.isEmpty()){
            return Result2.success(AuthorityList);
        }else {
            return Result2.error("查询角色所有权限失败!");
        }
    }
    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:set')")
    @PutMapping("/updateRoleAuthority")
    @ApiOperation("更改角色权限")
    public Result2 updateRoleAuthority(@RequestParam Long roleId, @RequestParam List<Long> authorityIds) {

        // 如果 authorityIds 为空，删除 roleId 对应的所有权限记录
        if (authorityIds == null || authorityIds.isEmpty()) {
            roleAuthorityService.removeById(roleId);

            // 👇 新增：清空权限时也推送刷新
            List<Long> userIds = userRoleService.getUserIdsByRoleId(roleId);
            for (Long userId : userIds) {
                WebSocketServerImpl.sendToUser(userId, "refreshPermissions");
            }

            return Result2.success("删除角色所有权限成功！");
        }

        // 获取当前 roleId 对应的权限列表
        List<Long> currentAuthorityIds = roleAuthorityService.getAuthoritysByRoleIds(roleId);

        // 删除多余的权限记录
        for (Long currentAuthorityId : currentAuthorityIds) {
            if (!authorityIds.contains(currentAuthorityId)) {
                roleAuthorityService.deleteByRoleIdAndAuthorityId(roleId, currentAuthorityId);
            }
        }

        // 添加没有的权限记录
        for (Long authorityId : authorityIds) {
            if (!currentAuthorityIds.contains(authorityId)) {
                RoleAuthority roleAuthority = new RoleAuthority();
                roleAuthority.setRoleId(roleId);
                roleAuthority.setAuthorityId(authorityId);
                roleAuthorityService.save(roleAuthority);
            }
        }

        // ====================== 新增：推送刷新给拥有该角色的所有用户 ======================
        List<Long> userIds = userRoleService.getUserIdsByRoleId(roleId);
        for (Long userId : userIds) {
            WebSocketServerImpl.sendToUser(userId, "refreshPermissions");
        }

        return Result2.success("更改角色权限成功！");
    }
    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:set')")
    @PostMapping("/insert")
    @ApiOperation("插入角色")
    public Result2 addUser(@RequestBody Role role){
        if (roleService.save(role)){
            return Result2.success("插入角色成功！");
        }
        else return Result2.error("插入角色失败");
    }
    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:remove')")
    @DeleteMapping("/delete/{roleId}")
    @ApiOperation("删除角色")
    public Result2 deleteUserById(@PathVariable Integer roleId) {
        if (roleService.removeById(roleId)) {
            return Result2.success("删除角色成功！");
        } else {
            return Result2.error("删除角色失败！");
        }
    }
    @GetMapping("/AuthoritiesByUserId")
    public Result2 getAuthoritiesByUserId(@RequestParam Long userId) {
        List<Long> roleIds = userRoleService.getRolesByUserId(userId);
        List<Long> authIds = roleAuthorityService.getAuthoritysByRoleIds(roleIds);
        return Result2.success(authIds);
    }
}
