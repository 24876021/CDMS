package com.example.userlogin.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * @author shuangtian
 */
@Data
public class SysUser {

    @TableId(type = IdType.AUTO)// 配置主键字段的生成策略为数据库自增。这样，插入新记录时，数据库会自动生成主键值
    //同时密码验证时也要查询主键，因此该标签必备

    private Long userId;

    private String name;

    private String account;

    private String password;

    private boolean status;

    private boolean disable;

}
