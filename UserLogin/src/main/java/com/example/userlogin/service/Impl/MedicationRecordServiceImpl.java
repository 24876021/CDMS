package com.example.userlogin.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.userlogin.mapper.MedicationRecordMapper;
import com.example.userlogin.model.MedicationRecord;
import com.example.userlogin.service.MedicationRecordService;
import org.springframework.stereotype.Service;

@Service

public class MedicationRecordServiceImpl extends ServiceImpl<MedicationRecordMapper, MedicationRecord> implements MedicationRecordService {
    @Override
    public MedicationRecord getByRecordId(Long recordId) {
        return this.getOne(new LambdaQueryWrapper<MedicationRecord>().eq(MedicationRecord::getRecordId,recordId));
    }

    @Override
    public boolean isRecordIdExists(Long recordId) {
        return this.count(new LambdaQueryWrapper<MedicationRecord>().eq(MedicationRecord::getRecordId, recordId)) > 0;
    }
}
