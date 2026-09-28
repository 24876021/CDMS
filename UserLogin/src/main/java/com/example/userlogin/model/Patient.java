package com.example.userlogin.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data

public class Patient {
    @TableId(type = IdType.AUTO)
    private int patientId;
    private String name;
    private String gender;
    private String birthDate;
    private int age;
    private String idCardNumber;
    private String contactNumber;
    private String address;
}
