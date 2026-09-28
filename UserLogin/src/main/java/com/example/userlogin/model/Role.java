package com.example.userlogin.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class Role {
    @TableId(type = IdType.AUTO)
    private Long roleId;

    private String roleName;

    private String description;
}
