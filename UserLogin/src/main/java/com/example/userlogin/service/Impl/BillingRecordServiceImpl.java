package com.example.userlogin.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.userlogin.mapper.BillingRecordMapper;
import com.example.userlogin.model.BillingRecord;
import com.example.userlogin.model.SysUser;
import com.example.userlogin.service.BillingRecordService;
import org.springframework.stereotype.Service;

@Service
public class BillingRecordServiceImpl extends ServiceImpl<BillingRecordMapper, BillingRecord> implements BillingRecordService {
    @Override
    public BillingRecord getByRecordId(Long recordId) {
        return this.getOne(new LambdaQueryWrapper<BillingRecord>().eq(BillingRecord::getRecordId,recordId));
    }

    @Override
    public boolean isRecordIdExists(Long recordId) {
        return this.count(new LambdaQueryWrapper<BillingRecord>().eq(BillingRecord::getRecordId, recordId)) > 0;
    }
}
