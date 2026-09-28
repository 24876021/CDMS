package com.example.userlogin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.userlogin.model.MedicalRecord;
import com.example.userlogin.model.Result2;
import com.example.userlogin.service.MedicalRecordService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MedicalRecordController {
    @Autowired
    MedicalRecordService medicalRecordService;

    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:get')")
    @GetMapping("/AllMedicalRecords")
    @ApiOperation("查询所有病例")
    public Result2 getAllUsers() {
        List<MedicalRecord> medicalRecordList = medicalRecordService.list();//查询所有对象实体并存入列表
        if (!medicalRecordList.isEmpty()) {
            return Result2.success(medicalRecordList);
        } else {
            return Result2.error("查询所有病历失败!");
        }
    }

    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:get')")
    @GetMapping("/MedicalRecord")
    @ApiOperation("根据 patientId 获取指定病例")
    public Result2 getMedicalRecord(@RequestParam(value = "patientId") Long patientId) {
        MedicalRecord medicalRecord = medicalRecordService.getByPatientId(patientId);
        if (medicalRecord != null) {
            return Result2.success(medicalRecord);
        } else {
            return Result2.error("未找到指定病例");
        }
    }

    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:get')")
    @GetMapping("/MedicalRecordPage")
    @ApiOperation("分页查询患者病历")
    public Result2 getAllUsers(@RequestParam(defaultValue = "1") int current,
                               @RequestParam(defaultValue = "1") int size,
                               @RequestParam(value = "patientId") Long patientId) {
        Page<MedicalRecord> page = new Page<>(current, size);
        IPage<MedicalRecord> MedicalRecordPage = medicalRecordService.getPatientRecordsByPage(page,patientId);
        if (MedicalRecordPage.getRecords().isEmpty()) {
            return Result2.error("查询患者病历失败!");
        } else {
            return Result2.success(MedicalRecordPage);
        }
    }

    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:set')")
    @PostMapping("/addMedicalRecord")
    @ApiOperation("添加病历记录")
    public Result2 addMedicalRecord(@RequestBody MedicalRecord medicalRecord) {
        if (medicalRecordService.save(medicalRecord)) {
            return Result2.success("添加病历记录成功！");
        } else {
            return Result2.error("添加病历记录失败");
        }
    }
}

