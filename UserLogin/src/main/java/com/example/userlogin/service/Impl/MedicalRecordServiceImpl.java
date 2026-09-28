package com.example.userlogin.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.userlogin.mapper.MedicalRecordMapper;
import com.example.userlogin.model.MedicalRecord;
import com.example.userlogin.model.SysUser;
import com.example.userlogin.service.MedicalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MedicalRecordServiceImpl extends ServiceImpl<MedicalRecordMapper, MedicalRecord> implements MedicalRecordService {
    @Autowired
    MedicalRecordMapper medicalRecordMapper;
    @Override
    public IPage<MedicalRecord> getPatientRecordsByPage(Page<MedicalRecord> page, Long patientId) {
        LambdaQueryWrapper<MedicalRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(MedicalRecord::getPatientId, patientId);
        return medicalRecordMapper.selectPage(page, queryWrapper);
    }

    @Override
    public MedicalRecord getByPatientId(Long patientId) {
        return this.getOne(new LambdaQueryWrapper<MedicalRecord>().eq(MedicalRecord::getPatientId,patientId));
    }
}
