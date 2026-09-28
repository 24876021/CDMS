package com.example.userlogin.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.userlogin.model.Patient;
import com.example.userlogin.model.SysUser;
import com.example.userlogin.mapper.SysUserMapper;
import com.example.userlogin.service.SysUserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    @Override
    public List<SysUser> getByUsername(String name) {
        return this.list(new LambdaQueryWrapper<SysUser>().eq(SysUser::getName, name));
        //通过传入的用户昵称查找有关的记录(可能有重名所以用列表
    }


    @Override
    public SysUser getByAccount(String account) {
        return this.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getAccount,account));
    }//查询SysUser数据库中account字段的值与参数account相同的记录并返回SysUser对象

}
