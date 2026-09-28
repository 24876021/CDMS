package com.example.userlogin.controller;

import com.example.userlogin.model.MedicationRecord;
import com.example.userlogin.model.Result2;
import com.example.userlogin.service.MedicationRecordService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class MedicationRecordController {
    @Autowired
    MedicationRecordService medicationRecordService;

    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:get')")
    @GetMapping("/MedicationRecord")
    @ApiOperation("根据 recordId 获取指定用药记录")
    public Result2 getMedicalRecord(@RequestParam(value = "recordId") Long recordId) {
        MedicationRecord medicationRecord = medicationRecordService.getByRecordId(recordId);
        if (medicationRecord != null) {
            return Result2.success(medicationRecord);
        } else {
            return Result2.error("未找到指定用药记录");
        }
    }

    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:set')")
    @PostMapping("/addMedicationRecord")
    @ApiOperation("添加用药记录")
    public Result2 addMedicationRecord(@RequestBody MedicationRecord medicationRecord) {
        // 判断 recordId 是否重复
        if (medicationRecordService.isRecordIdExists(medicationRecord.getRecordId())) {
            return Result2.error("该病历的用药记录已存在!");
        }

        if (medicationRecordService.save(medicationRecord)) {
            return Result2.success("添加用药记录成功！");
        } else {
            return Result2.error("添加用药记录失败");
        }
    }
}
