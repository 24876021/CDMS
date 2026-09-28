package com.example.userlogin.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.userlogin.mapper.PatientMapper;
import com.example.userlogin.model.Patient;
import com.example.userlogin.model.SysUser;
import com.example.userlogin.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientServiceImpl extends ServiceImpl<PatientMapper, Patient> implements PatientService {
    @Autowired
    PatientMapper patientMapper;
    @Override
    public List<Patient> getUsersByName(String name) {
        return this.list(new LambdaQueryWrapper<Patient>().eq(Patient::getName, name));
        //通过传入的患者姓名查找有关的记录(可能有重名所以用列表
    }
    @Override
    public Patient getByIdCardNumber(String idCardNumber) {
        return this.getOne(new LambdaQueryWrapper<Patient>().eq(Patient::getIdCardNumber,idCardNumber));
    }//靠身份证获取记录
    @Override
    public IPage<Patient> getAllPatientsByPage(Page<Patient> page) {
        return patientMapper.selectPage(page, null);
    }//分页查询所有记录
    @Override
    public IPage<Patient> getPatientsByPage(Page<Patient> page, String name) {
        LambdaQueryWrapper<Patient> queryWrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            queryWrapper.like(Patient::getName, name);
        }
        return patientMapper.selectPage(page, queryWrapper);
    }
}
