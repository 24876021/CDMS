package com.example.userlogin.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;

@Data
public class Authority implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)

    private Long authorityId;

    private String authorityName;

    private String description;

}
