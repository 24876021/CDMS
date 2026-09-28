package com.example.userlogin.controller;

import com.example.userlogin.model.BillingRecord;
import com.example.userlogin.model.Result2;
import com.example.userlogin.service.BillingRecordService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class BillingRecordController {
    @Autowired
    BillingRecordService billingRecordService;

    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:get')")
    @GetMapping("/BillingRecord")
    @ApiOperation("根据 recordId 获取指定费用记录")
    public Result2 getMedicalRecord(@RequestParam(value = "recordId") Long recordId) {
        BillingRecord billingRecord = billingRecordService.getByRecordId(recordId);
        if (billingRecord != null) {
            return Result2.success(billingRecord);
        } else {
            return Result2.error("未找到指定费用记录");
        }
    }

    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:set')")
    @PostMapping("/addBillingRecord")
    @ApiOperation("添加费用记录")
    public Result2 addBillingRecord(@RequestBody BillingRecord billingRecord) {
        // 判断 recordId 是否重复
        if (billingRecordService.isRecordIdExists(billingRecord.getRecordId())) {
            return Result2.error("该病历的费用记录已存在!");
        }

        if (billingRecordService.save(billingRecord)) {
            return Result2.success("添加费用记录成功！");
        } else {
            return Result2.error("添加费用记录失败");
        }
    }
}
