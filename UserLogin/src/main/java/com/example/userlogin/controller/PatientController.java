package com.example.userlogin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.userlogin.model.*;
import com.example.userlogin.service.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@Api(tags = "患者管理相关接口")
@RequestMapping("/patient")
public class PatientController {
    @Autowired
    PatientService patientService;
    @Autowired
    SysUserService sysUserService;
    @Autowired
    UserRoleService userRoleService;


    /**
     * 查询所有患者
     * @return
     */
    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:get')")
    @GetMapping("/AllUsers")
    @ApiOperation("查询所有患者")
    public Result2 getAllUsers(){
        List<Patient> patientList = patientService.list();//查询所有对象实体并存入列表
        if (!patientList.isEmpty()){
            return Result2.success(patientList);
        }else {
            return Result2.error("查询所有患者失败!");
        }
    }
    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:get')")
    @GetMapping("/AllUsersPage")
    @ApiOperation("分页查询所有患者")
    public Result2 getAllUsers(@RequestParam(defaultValue = "1") int current,
                               @RequestParam(defaultValue = "10") int size) {
        Page<Patient> page = new Page<>(current, size);
        IPage<Patient> patientPage = patientService.getAllPatientsByPage(page);
        if (patientPage.getRecords().isEmpty()) {
            return Result2.error("查询所有患者失败!");
        } else {
            return Result2.success(patientPage);
        }
    }

    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:get')")
    @GetMapping("/idCardNumber")
    @ApiOperation("根据 idCardNumber 查询指定患者")
    public Result2 getPatient(@RequestParam(value = "idCardNumber") String idCardNumber) {
        Patient patient = patientService.getByIdCardNumber(idCardNumber);
        if (patient != null) {
            return Result2.success(patient);
        } else {
            return Result2.error("未找到指定患者");
        }
    }

    /**
     * 根据 id 查询指定患者
     * @param patientId 患者ID
     * @param name 患者姓名
     * @return
     */
    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:get')")
    @GetMapping("/user")
    @ApiOperation("根据 patientId 或 name 查询指定患者")
    public Result2 getUserByIdOrName(@RequestParam(value = "patientId", required = false) Integer patientId,
                                     @RequestParam(value = "name", required = false) String name) {
        Patient patient = null;
        List<Patient> patientList =null;
        if (patientId != null) {
            patient = patientService.getById(patientId);
        } else if (name != null) {
            patientList = patientService.getUsersByName(name);
        } else {
            return Result2.error("请提供 patientId 或 name 参数");
        }

        if (patient == null && patientList ==null) {
            return Result2.error("未找到指定患者");
        }

        if (patient != null) {
            return Result2.success(patient);
        } else {
            return Result2.success(patientList);
        }
    }

    /**
     * 插入患者
     * @param patient
     * @return
     */
    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:set')")
    @PostMapping("/user")
    @ApiOperation("插入患者")
    public Result2 addUser(@RequestBody Patient patient) {
        String idCard = patient.getIdCardNumber();
        if (idCard == null || idCard.trim().isEmpty()) {
            return Result2.error("身份证不能为空");
        }

        Patient exist = patientService.getByIdCardNumber(idCard);
        if (exist != null) {
            return Result2.error("该身份证已存在");
        }

        if (patientService.save(patient)) {
            return Result2.success("插入成功");
        }
        return Result2.error("插入失败");
    }

    /**
     * 修改用户信息
     * @param patient
     * @return
     */
    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:set')")
    @PutMapping("/user")
    @ApiOperation(("修改患者"))
    public Result2 updateUser(@RequestBody Patient patient){

        // 1. 身份证判空
        String idCardNumber = patient.getIdCardNumber();
        if (idCardNumber == null || idCardNumber.trim().isEmpty()) {
            return Result2.error("身份证号不能为空");
        }

        // 2. 查询是否已存在这个身份证
        Patient existPatient = patientService.getByIdCardNumber(idCardNumber);

        // 3. 关键：如果存在，并且不是当前患者自己 → 拦截
        if (existPatient != null) {
            // 重点：int 类型必须用 == 比较
            if (existPatient.getPatientId() != patient.getPatientId()) {
                return Result2.error("该身份证号已被其他患者使用！");
            }
        }

        // 4. 校验通过才执行更新
        try {
            if (patientService.updateById(patient)){
                return Result2.success("更新患者成功！");
            } else {
                return Result2.error("更新失败");
            }
        } catch (Exception e) {
            return Result2.error("该身份证号已被占用");
        }
    }

    /**
     * 删除患者
     * @param patientId 患者ID
     * @return 操作信息
     */
    @PreAuthorize("hasAuthority('resource:all')||hasAuthority('resource:remove')")
    @DeleteMapping("/delete/{patientId}")
    @ApiOperation("删除患者")
    public Result2 deleteUserById(@PathVariable Integer patientId) {
        if (patientService.removeById(patientId)) {
            return Result2.success("删除患者成功！");
        } else {
            return Result2.error("删除患者失败！");
        }
    }
}