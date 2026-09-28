package com.example.userlogin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.userlogin.model.MedicationRecord;

public interface MedicationRecordService extends IService<MedicationRecord> {
    MedicationRecord getByRecordId(Long recordId);

    boolean isRecordIdExists(Long recordId);
}
