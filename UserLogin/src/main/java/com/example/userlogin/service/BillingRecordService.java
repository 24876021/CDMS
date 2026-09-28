package com.example.userlogin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.userlogin.model.BillingRecord;

public interface BillingRecordService extends IService<BillingRecord> {
    public BillingRecord getByRecordId(Long recordId);

    public boolean isRecordIdExists(Long recordId);
}
