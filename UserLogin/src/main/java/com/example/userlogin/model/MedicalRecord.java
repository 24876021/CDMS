package com.example.userlogin.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * 病例表
 * */
@Data
public class MedicalRecord {

    @TableId(type = IdType.AUTO)
    private Long recordId;

    private Long patientId;

    private String visitDate;

    private String chiefComplaint;

    private String presentIllness;

    private String pastHistory;

    private String familyHistory;

    private String physicalExamination;

    private String diagnosis;

    private String treatmentPlan;

    private String dischargeDate;

    private String dischargeDiagnosis;

    private String dischargeInstructions;
}
