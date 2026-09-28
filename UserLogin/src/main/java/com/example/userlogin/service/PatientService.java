package com.example.userlogin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.userlogin.model.Patient;

import java.util.List;

public interface PatientService extends IService<Patient> {
    Patient getByIdCardNumber(String idCardNumber);
    public List<Patient> getUsersByName(String name);
    public IPage<Patient> getAllPatientsByPage(Page<Patient> page);
    public IPage<Patient> getPatientsByPage(Page<Patient> page, String name);
}
