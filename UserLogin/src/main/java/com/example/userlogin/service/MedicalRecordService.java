package com.example.userlogin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.userlogin.model.MedicalRecord;

public interface MedicalRecordService extends IService<MedicalRecord> {
    public IPage<MedicalRecord> getPatientRecordsByPage(Page<MedicalRecord> page, Long patientId);

    public MedicalRecord getByPatientId(Long patientId);
}
