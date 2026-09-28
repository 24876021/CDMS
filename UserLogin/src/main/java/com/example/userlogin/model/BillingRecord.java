package com.example.userlogin.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class BillingRecord {

    @TableId(type = IdType.AUTO)
    private Long billingId;

    private Long recordId;

    private String itemName;

    private String itemCost;

    private String paymentMethod;

    private String insuranceInfo;
}
