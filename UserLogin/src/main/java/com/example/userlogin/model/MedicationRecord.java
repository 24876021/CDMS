package com.example.userlogin.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class MedicationRecord {

    @TableId(type = IdType.AUTO)//插入记录时让主键自增
    private Long medicationId;

    private Long recordId;

    private String drugName;

    private String dosage;

    private String frequency;

    private String startDate;

    private String endDate;
}
