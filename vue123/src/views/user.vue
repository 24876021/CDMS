<template>

  <div class="page-background">
    <el-container class="app-main-wrap">
      <el-header>
        <div class="title-wrap">
          <h1 class="title">
            <img src="@/assets/user1.png" class="logo" />
            社区医疗诊断管理
          </h1>
        </div>
      </el-header>
      <el-main>

        <el-row v-if="activeTab === 'first'">
          <el-col :span="12">
            <el-input v-if="hasGetPermission" v-model="searchQuery" placeholder="请输入患者ID或姓名" clearable style="width: 200px;" @keydown.enter.native="searchUser"></el-input>
            <el-button v-if="hasGetPermission" type="primary" @click="searchUser">查询</el-button>
          </el-col>
        </el-row>

        <el-row v-if="activeTab === 'five'">
          <el-col :span="12">
        <el-input v-if="hasGetPermission" v-model="idCardNumber" placeholder="请输入身份证号" clearable style="width: 200px;" />
        <el-button v-if="hasGetPermission" type="primary" @click="searchByIdCardNumber">查询</el-button>
          </el-col>
        </el-row>

        <el-tabs v-model="activeTab">
          <el-tab-pane v-if=hasDoctorRole label="患者信息" name="first">
            <el-button v-if="hasGetPermission" type="primary" icon="el-icon-refresh-right" @click="getAllUsers(true)">刷新</el-button>
            <div style="margin-top: 20px;"></div>
            <el-table :data="patients" style="width: 100%; border-radius: 5px;">
              <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label" :width="col.width" align="center">
                <template slot-scope="scope">
                  <el-input v-if="scope.row.editing" v-model="scope.row[col.prop]"></el-input>
                  <span v-else>{{ scope.row[col.prop] }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作">
                <template slot-scope="scope">
                  <el-button v-if="hasRemovePermission" type="text" icon="el-icon-delete" @click="deleteUser(scope.row.patientId)">删除</el-button>
                  <el-button v-if=hasSetPermission type="text" :icon="scope.row.editing ? 'el-icon-check' : 'el-icon-edit'" @click="editUser(scope.row)">
                    {{ scope.row.editing ? '保存' : '修改' }}
                  </el-button>
                  <el-button v-if="hasGetPermission" type="text" icon="el-icon-search" @click="openMedicalRecordDialog(scope.row.patientId)">病历查询</el-button>
                  <el-button v-if="hasSetPermission" type="text" icon="el-icon-plus" @click="openAddMedicalRecordDialog(scope.row.patientId)">添加病历</el-button>
                </template>
              </el-table-column>
            </el-table>
            <!--分页控件-->
            <el-pagination
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
                :current-page="currentPage"
                :page-sizes="[5, 10, 20,50, 100]"
                :page-size="pageSize"
                layout="total, sizes, prev, pager, next, jumper"
                :total="total">
            </el-pagination>
          </el-tab-pane>
          <el-tab-pane v-if=hasAdminRole label="用户信息" name="second">
            <div style="margin-top: 20px;"></div>
            <el-row :gutter="20">
              <el-col :span="6" v-for="(user) in users" :key="user.userId">
                <div style="margin-bottom: 20px;"></div>
                <el-card class="box-card">
                  <div slot="header" class="clearfix">
                    <span>{{ user.name }}</span>
                  </div>
                  <div v-for="col in userColumns" :key="col.prop" class="text item">
                    <span>{{ col.label }}: {{ user[col.prop] }}</span>
                    <el-switch
                        v-if="col.prop === 'status' || col.prop === 'disable'"
                        v-model="user[col.prop]"
                        active-value="true"
                        inactive-value="false"
                        @change="updateUserStatus(user.userId, col.prop, user[col.prop])"
                    ></el-switch>
                  </div>
                  <div>
                    <el-checkbox-group v-model="user.selectedRoles" @change="updateUserRoles(user)">
                      <el-checkbox v-for="role in roles" :key="role.roleId" :label="role.roleId">
                        {{ role.description }}
                      </el-checkbox>
                    </el-checkbox-group>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </el-tab-pane>
          <el-tab-pane v-if=hasDoctorRole label="添加患者" name="third">
            <el-form label-width="80px" :model="addUserParams" :rules="rules" ref="addUserForm">
              <el-form-item v-for="item in formItems" :key="item.prop" :label="item.label" :prop="item.prop">
                <el-input v-if="item.type === 'input'" v-model="addUserParams[item.prop]" :placeholder="item.placeholder" clearable style="width: 200px;"></el-input>
                <el-select v-else-if="item.type === 'select'" v-model="addUserParams[item.prop]" :placeholder="item.placeholder" style="width: 200px;">
                  <el-option v-for="option in options" :key="option.value" :label="option.label" :value="option.value"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="addUser">添加<i class="el-icon-upload el-icon--right"></i></el-button>
              </el-form-item>
              简介：<br>患者信息填写<br>
            </el-form>
            <div class="hg"></div>
          </el-tab-pane>
          <el-tab-pane v-if=hasAdminRole label="角色管理" name="four">
            <div style="margin-top: 20px;"></div>

            <!-- 列出所有角色 -->
            <el-row :gutter="20">
              <el-col :span="6" v-for="role in roles" :key="role.roleId">
                <div style="margin-bottom: 20px;"></div>
                <el-card class="box-card">
                  <div slot="header" class="clearfix">
                    <span>{{ role.description }}</span>
                  </div>
                  <div>
                    <el-checkbox-group v-model="role.selectedAuthorities" @change="updateRolePermissions(role)">
                      <el-checkbox
                          v-for="authority in allAuthorities"
                          :key="authority.authorityId"
                          :label="authority.authorityId">
                        {{ authority.description }}
                      </el-checkbox>
                    </el-checkbox-group>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </el-tab-pane>
          <el-tab-pane v-if="hasPatientRole" label="指定患者信息" name="five">
            <el-table :data="selectedPatient" style="width: 100%; border-radius: 5px;">
              <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label" :width="col.width" align="center">
              </el-table-column>
              <el-table-column label="操作">
                <template slot-scope="scope">
                  <el-button type="success" icon="el-icon-search" @click="openMedicalRecordDialog(scope.row.patientId)">病历查询</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-main>
    </el-container>
    <!-- 右上角用户信息列表 -->
    <div class="top-right-dropdown">
      <el-dropdown @command="handleCommand">
        <span class="el-dropdown-link">
          {{ currentUser.name }}<i class="el-icon-arrow-down el-icon--right"></i>
        </span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item v-for="item in userInfoItems" :key="item.prop" :command="item.prop">
            {{ item.label }}: {{ currentUser[item.prop] }}
          </el-dropdown-item>
          <el-dropdown-item command="logout">登出</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
    <!-- 病历信息弹窗 -->
    <el-dialog
        title="病历信息"
        :visible.sync="dialogVisible"
        width="50%"
        @close="dialogVisible = false"
    >
      <el-form label-width="120px">
<!--嵌套了一层<el-form-item>是为了正确地绑定数组（因为el-form不能直接绑定数组，只能绑定对象。。）-->
        <el-form-item
            v-for="(record, index) in medicalRecord"
            :key="index"
            class="form-item-border"
        >
          <el-button v-if="hasGetPermission" type="success" icon="el-icon-money" @click="openBillingRecordDialog(record.recordId)">费用查询</el-button>
          <el-button v-if="hasSetPermission" type="primary" @click="openAddBillingRecordDialog(record.recordId)">添加费用信息</el-button>
          <el-button v-if="hasGetPermission" type="success" icon="el-icon-money" @click="openMedicationRecordDialog(record.recordId)">用药查询</el-button>
          <el-button v-if="hasSetPermission" type="primary" @click="openAddMedicationRecordDialog(record.recordId)">添加用药信息</el-button>
          <el-form-item
              v-for="col in medicalColumns"
              :key="col.prop"
              :label="col.label"
              :prop="col.prop"
              class="form-item-border"
          >
            {{ medicalRecord[index][col.prop] }}
          </el-form-item>
        </el-form-item>
      </el-form>
      <!-- 病历信息分页 -->
      <el-pagination
          @size-change="handleMedicalRecordSizeChange"
          @current-change="handleMedicalRecordCurrentChange"
          :current-page="medicalCurrentPage"
          :page-sizes="[1, 2, 3, 4, 5]"
          :page-size="medicalPageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="totalRecords"
      ></el-pagination>
    </el-dialog>

    <!-- 添加病历信息弹窗 -->
    <el-dialog
        title="添加病历"
        :visible.sync="addMedicalRecordDialogVisible"
        width="50%"
        @close="addMedicalRecordDialogVisible = false"
    >
      <el-form label-width="120px" :model="medicalRecord" :rules="addMedicalRecordRules" ref="addMedicalRecordForm">
        <el-form-item v-for="(item, index) in medicalColumns" :key="index" :label="item.label" :prop="item.prop">
          <el-input v-model="medicalRecord[item.prop]"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitAddMedicalRecord">提交</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <!-- 费用信息弹窗 -->
    <el-dialog
        title="费用信息"
        :visible.sync="billingDialogVisible"
        width="50%"
        @close="billingDialogVisible = false"
    >
      <el-button
          slot="title"
          icon="el-icon-arrow-left"
          @click="closeBillingRecordDialog"
      >
        返回
      </el-button>

      <el-form label-width="120px">
        <el-form-item v-for="(col, index) in billingColumns" :key="index">
          <el-form-item :label="col.label" :prop="col.prop" class="form-item-border">
            {{ billingRecord[col.prop] }}
          </el-form-item>
        </el-form-item>
      </el-form>
    </el-dialog>

    <el-dialog
        title="费用信息添加"
        :visible.sync="addBillingRecordDialogVisible"
        width="50%"
        @close="addBillingRecordDialogVisible = false"
    >
      <el-form label-width="120px" :model="billingRecord" :rules="billingRecordRules" ref="billingRecordForm">
        <el-form-item v-for="(col, index) in billingColumns" :key="index">
          <el-form-item :label="col.label" :prop="col.prop" class="form-item-border">
            <el-input v-model="billingRecord[col.prop]"></el-input>
          </el-form-item>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitBillingRecord">提交</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <!-- 用药信息弹窗 -->
    <el-dialog
        title="用药信息"
        :visible.sync="medicationDialogVisible"
        width="50%"
        @close="medicationDialogVisible = false"
    >
      <el-button
          slot="title"
          icon="el-icon-arrow-left"
          @click="closeMedicationRecordDialog"
      >
        返回
      </el-button>

      <el-form label-width="120px">
        <el-form-item v-for="(col, index) in medicationColumns" :key="index">
          <el-form-item :label="col.label" :prop="col.prop" class="form-item-border">
            {{ medicationRecord[col.prop] }}
          </el-form-item>
        </el-form-item>
      </el-form>
    </el-dialog>

    <el-dialog
        title="用药信息添加"
        :visible.sync="addMedicationRecordDialogVisible"
        width="50%"
        @close="addMedicationRecordDialogVisible = false"
    >
      <el-form label-width="120px" :model="medicationRecord" :rules="medicationRecordRules" ref="medicationRecordForm">
        <el-form-item v-for="(col, index) in medicationColumns" :key="index">
          <el-form-item :label="col.label" :prop="col.prop" class="form-item-border">
            <el-input v-model="medicationRecord[col.prop]"></el-input>
          </el-form-item>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitMedicationRecord">提交</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

  </div>
</template>
<script>
export default {
  name: "PatientInfo",
  data() {
    return {
      roles: [], // 所有角色列表
      allAuthorities: [], // 所有权限列表
      patients: [],
      //身份证指定患者存储
      selectedPatient:[],
      users: [],
      addUserParams: {
        patientId: null,
        name: null,
        gender: null,
        birthDate: null,
        age: null,
        idCardNumber: null,
        contactNumber: null,
        address: null
      },
      options: [{ value: '男', label: '男' }, { value: '女', label: '女' }],
      searchQuery: '',
      idCardNumber:'',
      columns: [
        { prop: 'patientId', label: '患者ID', width: '90px' },
        { prop: 'name', label: '患者姓名', width: '100px' },
        { prop: 'gender', label: '性别', width: '90px' },
        { prop: 'birthDate', label: '出生日期', width: '150px' },
        { prop: 'age', label: '年龄', width: '90px' },
        { prop: 'idCardNumber', label: '身份证号', width: '220px' },
        { prop: 'contactNumber', label: '联系电话' , width: '220px'},
        { prop: 'address', label: '地址' }
      ],

      dialogVisible: false,// 病历信息弹窗可见性
      billingDialogVisible: false, // 费用信息弹窗可见性
      medicationDialogVisible: false,// 用药信息弹窗可见性
      addMedicalRecordDialogVisible: false,
      addBillingRecordDialogVisible: false,
      addMedicationRecordDialogVisible: false,

      medicalRecord: {}, // 病历记录数据
      billingRecord: {}, // 费用记录数据
      medicationRecord: {
        medicationId:null
      }, // 用药记录数据

      medicalColumns: [
        { prop: 'visitDate', label: '就诊日期：' },
        { prop: 'chiefComplaint', label: '主诉：' },
        { prop: 'presentIllness', label: '现病史：' },
        { prop: 'pastHistory', label: '既往史：' },
        { prop: 'familyHistory', label: '家族史：' },
        { prop: 'physicalExamination', label: '体格检查：' },
        { prop: 'diagnosis', label: '诊断：' },
        { prop: 'treatmentPlan', label: '治疗方案：' },
        { prop: 'dischargeDate', label: '出院日期：' },
        { prop: 'dischargeDiagnosis', label: '出院诊断：' },
        { prop: 'dischargeInstructions', label: '出院医嘱：' }
      ],
      billingColumns: [
        { prop: 'itemName', label: '费用项目名称：' },
        { prop: 'itemCost', label: '费用金额：' },
        { prop: 'paymentMethod', label: '支付方式：' },
        { prop: 'insuranceInfo', label: '保险信息：' }
      ],
      medicationColumns:[
        { prop: 'drugName', label: '药品名称：' },
        { prop: 'dosage', label: '剂量：' },
        { prop: 'frequency', label: '用药频率：' },
        { prop: 'startDate', label: '开始日期：' },
        { prop: 'endDate', label: '结束日期：' }
      ],
      userColumns: [
        { prop: 'userId', label: '用户ID', width: '70px' },
        { prop: 'name', label: '用户昵称', width: '80px' },
        { prop: 'account', label: '账号', width: '100px' },
        { prop: 'status', label: '是否正常', width: '60px' },
        { prop: 'disable', label: '是否启用', width: '80px' }
      ],
      //添加患者信息表格列
      formItems: [
        { prop: 'name', label: '姓名', type: 'input', placeholder: '请输入内容' },
        { prop: 'gender', label: '性别', type: 'select', placeholder: '请选择' },
        { prop: 'birthDate', label: '出生日期', type: 'input', placeholder: '请输入内容' },
        { prop: 'age', label: '年龄', type: 'input', placeholder: '请输入内容' },
        { prop: 'idCardNumber', label: '身份证号', type: 'input', placeholder: '请输入内容' },
        { prop: 'contactNumber', label: '联系电话', type: 'input', placeholder: '请输入内容' },
        { prop: 'address', label: '地址', type: 'input', placeholder: '请输入内容' }
      ],
      rules: {
        name: [
          { required: true, message: '请输入姓名', trigger: 'blur' }
        ],
        gender: [
          { required: true, message: '请选择性别', trigger: 'change' }
        ],
        birthDate: [
          { required: true, message: '请输入出生日期', trigger: 'blur' }
        ],
        age: [
          { required: true, message: '请输入年龄', trigger: 'blur' }
        ],
        idCardNumber: [
          { required: true, message: '请输入身份证号', trigger: 'blur' }
        ],
        contactNumber: [
          { required: true, message: '请输入联系电话', trigger: 'blur' }
        ],
        address: [
          { required: true, message: '请输入地址', trigger: 'blur' }
        ]
      },
      billingRecordRules:{
        itemName: [
          { required: true, message: '请输入费用项目名称', trigger: 'blur' }
        ],
        itemCost: [
          { required: true, message: '请输入费用金额', trigger: 'blur' }
        ],
        paymentMethod: [
          { required: true, message: '请输入支付方式', trigger: 'blur' }
        ],
        insuranceInfo: [
          { required: true, message: '请输入保险信息', trigger: 'blur' }
        ]
      },
      medicationRecordRules:{
        drugName: [
          { required: true, message: '请输入药品名称', trigger: 'blur' }
        ],
        dosage: [
          { required: true, message: '请输入剂量', trigger: 'blur' }
        ],
        frequency: [
          { required: true, message: '请输入用药频率', trigger: 'blur' }
        ],
        startDate: [
          { required: true, message: '请输入开始日期', trigger: 'blur' }
        ],
        endDate: [
          { required: true, message: '请输入结束日期', trigger: 'blur' }
        ]
      },
      addMedicalRecordRules: {
        // 患者 ID 字段的验证规则
        patientId: [
          { required: true, message: '请输入患者 ID', trigger: 'blur' }
        ],
        // 就诊日期字段的验证规则
        visitDate: [
          { required: true, message: '请输入就诊日期', trigger: 'blur' }
        ],
        // 主诉字段的验证规则
        chiefComplaint: [
          { required: true, message: '请输入主诉', trigger: 'blur' }
        ],
        // 现病史字段的验证规则
        presentIllness: [
          { required: true, message: '请输入现病史', trigger: 'blur' }
        ],
        // 既往史字段的验证规则
        pastHistory: [
          { required: true, message: '请输入既往史', trigger: 'blur' }
        ],
        // 家族史字段的验证规则
        familyHistory: [
          { required: true, message: '请输入家族史', trigger: 'blur' }
        ],
        // 体格检查字段的验证规则
        physicalExamination: [
          { required: true, message: '请输入体格检查结果', trigger: 'blur' }
        ],
        // 诊断字段的验证规则
        diagnosis: [
          { required: true, message: '请输入诊断结果', trigger: 'blur' }
        ],
        // 治疗方案字段的验证规则
        treatmentPlan: [
          { required: true, message: '请输入治疗方案', trigger: 'blur' }
        ],
        // 出院日期字段的验证规则
        dischargeDate: [
          { required: true, message: '请输入出院日期', trigger: 'blur' }
        ],
        // 出院诊断字段的验证规则
        dischargeDiagnosis: [
          { required: true, message: '请输入出院诊断结果', trigger: 'blur' }
        ],
        // 出院医嘱字段的验证规则
        dischargeInstructions: [
          { required: true, message: '请输入出院医嘱', trigger: 'blur' }
        ]
      },
      activeTab: 'first',
      currentUser: [],
      userInfoItems: [
        { prop: 'userId', label: '用户ID' },
        { prop: 'name', label: '用户昵称' },
        { prop: 'account', label: '账号' },
        { prop: 'status', label: '是否正常' },
        { prop: 'disable', label: '是否启用' }
      ],
      currentPage: 1,
      pageSize: 10,
      total: 0,

      totalRecords: 0, // 病历总记录数
      medicalCurrentPage: 1,  // 当前病历页数
      medicalPageSize: 1,    // 每页病历条数
    }
  },
  mounted() {
    this.getAllUsers(false);
    this.getAllSysUsers(false);
    this.fetchCurrentUser();
    this.getRoles(); // 获取所有角色
    this.getAllAuthorities(); // 获取所有权限
  },
  computed: {
    userRoles() {
      return JSON.parse(localStorage.getItem("userRoles")) || [];
    },
    userAuthorities() {
      return JSON.parse(localStorage.getItem("userAuthorities")) || [];
    },
    hasGetPermission() {
      return this.userAuthorities.includes('resource:get')|| this.userAuthorities.includes('all');
    },
    hasSetPermission() {
      return this.userAuthorities.includes('resource:set')|| this.userAuthorities.includes('all');
    },
    hasRemovePermission() {
      return this.userAuthorities.includes('resource:remove')|| this.userAuthorities.includes('all');
    },
    hasAdminRole() {
      return this.userRoles.includes('admin')|| this.userRoles.includes('test 1');
    },
    hasDoctorRole() {
      return this.userRoles.includes('doctor')|| this.userRoles.includes('test 1');
    },
    hasPatientRole() {
      return this.userRoles.includes('patient')|| this.userRoles.includes('test 1');
    }
  },
  methods: {
    searchByIdCardNumber() {
      // 发送查询请求
      this.getRequest(`/patient/idCardNumber?idCardNumber=${this.idCardNumber}`)
          .then(response => {
            if (response.data.code === 200) {
              // 更新患者信息
              this.selectedPatient = [response.data.data];
            } else {
              // 处理错误
              this.$message.error(response.data.msg);
            }
          })
          .catch(error => {
            // 处理请求失败
            console.error('查询患者信息失败:', error);
            this.$message.error('查询患者信息失败');
          });
    },
    // 打开病历信息弹窗
    openMedicalRecordDialog(patientId) {
      this.selectedPatientId = patientId;  // 设置当前选中的患者ID
      this.queryMedicalRecord(patientId);  // 查询病历信息
      this.dialogVisible = true;  // 显示病历信息弹窗
    },
    // 打开添加病历弹窗
    openAddMedicalRecordDialog(patientId) {
      this.addMedicalRecordDialogVisible = true;
      this.medicalRecord = {
        // 初始化添加病历的表单数据
        patientId: patientId,
      };
    },
    // 打开费用信息弹窗
    openBillingRecordDialog(recordId) {
      this.queryBillingRecord(recordId); // 查询费用信息
      this.billingDialogVisible = true; // 显示费用弹窗
    },
    // 打开添加费用信息弹窗
    openAddBillingRecordDialog(recordId) {
      this.addBillingRecordDialogVisible = true;
      this.billingRecord.recordId = recordId;
    },
    // 关闭费用信息弹窗
    closeBillingRecordDialog() {
      this.billingDialogVisible = false; // 关闭费用弹窗
    },
    // 查询费用记录
    queryBillingRecord(recordId) {
      this.getRequest(`/BillingRecord?recordId=${recordId}`).then((resp) => {
        if (resp.data.code === 200) {
          this.billingRecord = resp.data.data; // 获取费用记录数据
        } else {
          this.$message.error(resp.data.msg);
        }
      }).catch((error) => {
        console.error('查询费用记录失败:', error);
        this.$message.error('查询费用记录失败');
      });
    },
    // 打开用药信息弹窗
    openMedicationRecordDialog(recordId) {
      this.queryMedicationRecord(recordId); // 查询费用信息
      this.medicationDialogVisible = true; // 显示费用弹窗
    },
    // 打开添加用药信息弹窗
    openAddMedicationRecordDialog(recordId) {
      this.addMedicationRecordDialogVisible = true;
      this.medicationRecord.recordId = recordId;
    },
    // 关闭用药信息弹窗
    closeMedicationRecordDialog() {
      this.medicationDialogVisible = false; // 关闭费用弹窗
    },
    // 查询用药记录
    queryMedicationRecord(recordId) {
      this.getRequest(`/MedicationRecord?recordId=${recordId}`).then((resp) => {
        if (resp.data.code === 200) {
          this.medicationRecord = resp.data.data; // 获取费用记录数据
        } else {
          this.$message.error(resp.data.msg);
        }
      }).catch((error) => {
        console.error('查询费用记录失败:', error);
        this.$message.error('查询费用记录失败');
      });
    },
    // 分页查询病历信息
    queryMedicalRecord(patientId) {
      this.getRequest(`/MedicalRecordPage?current=${this.medicalCurrentPage}&size=${this.medicalPageSize}&patientId=${patientId}`).then(resp => {
        if (resp.data.code === 200) {
          this.medicalRecord = resp.data.data.records;
          this.totalRecords = resp.data.data.total;
        } else {
          this.$message.error("未找到指定病历");
        }
      }).catch(error => {
        console.error('查询病历失败:', error);
        this.$message.error('查询病历失败');
      });
    },
    //分页获取所有患者信息
    getAllUsers(showMessage = true) {
      this.getRequest(`/patient/AllUsersPage?current=${this.currentPage}&size=${this.pageSize}`).then(resp => {
        if (resp.data.code === 200) {
          if (showMessage) this.$message.success("刷新成功!");
          this.patients = resp.data.data.records.map(patient => ({ ...patient, editing: false }));
          this.total = resp.data.data.total;
        } else if (resp.data.code === 400 && showMessage) {
          this.$message.error(resp.data.data);
        }
      })
    },
    // 获取所有角色
    getRoles() {
      this.getRequest("/role/AllRoles").then(resp => {
        if (resp.data.code === 200) {
          this.roles = resp.data.data.map(role => ({
            ...role,
            selectedAuthorities: [] // 初始化选中的权限
          }));
          this.loadRolePermissions();
        } else {
          this.$message.error("获取角色失败!");
        }
      });
    },
    // 获取所有权限
    getAllAuthorities() {
      this.getRequest("/role/AllAuthorities").then(resp => {
        if (resp.data.code === 200) {
          this.allAuthorities = resp.data.data;
        } else {
          this.$message.error("获取权限失败!");
        }
      });
    },
    // 遍历获取所有角色已有的权限
    loadRolePermissions() {
      this.roles.forEach(role => {
        this.getRequest(`/role/Authorities?roleId=${role.roleId}`).then(resp => {
          if (resp.data.code === 200) {
            role.selectedAuthorities = resp.data.data; // 默认选中权限
          } else {
            this.$message.error("有角色无权限!");
          }
        });
      });
    },
    //遍历获取所有用户已有的角色
    loadUserRoles() {
      this.users.forEach(user => {
        this.getRequest(`/sysUser/roles?userId=${user.userId}`).then(resp => {
          if (resp.data.code === 200) {
            user.selectedRoles = resp.data.data;
          } else {
            this.$message.error("有用户无角色!");
          }
        });
      });
    },
    // 更新角色权限
    updateRolePermissions(role) {
      this.putAuthorityRequest("/role/updateRoleAuthority", {
        roleId: role.roleId,
        authorityIds: role.selectedAuthorities.join(",")//要转化为逗号分隔的字符串再发送，直接发送卡了我半天。。
      }).then(resp => {
        if (resp.data.code === 200) {
          this.$message.success("更新角色权限成功!");
        } else {
          this.$message.error("更新角色权限失败!");
        }
      });
    },
    //更新用户角色
    updateUserRoles(user) {
      this.putAuthorityRequest('/sysUser/updateUserRole', {
        userId: user.userId,
        roleIds: user.selectedRoles.join(",")
      }).then(resp => {
        if (resp.data.code === 200) {
          this.$message.success("更新用户角色成功!");
        } else {
          this.$message.error("更新用户角色失败!");
        }
      });
    },
    //获取系统用户
    getAllSysUsers(showMessage = true) {
      this.getRequest("/sysUser/AllUsers").then(resp => {
        if (resp.data.code === 200) {
          if (showMessage) this.$message.success("刷新成功!");
          this.users = resp.data.data.map(user => ({
            ...user,
            editing: false,
            status: user.status.toString(),
            disable: user.disable.toString(),
            selectedRoles: []
          }));
          this.loadUserRoles();
        } else if (resp.data.code === 400 && showMessage) {
          this.$message.error(resp.data.data);
        }
      })
    },
    //添加患者
    addUser() {
      this.$refs.addUserForm.validate((valid) => {
        if (valid) {
          this.postRequest("/patient/user", this.addUserParams).then(resp => {
            if (resp.data.code === 200) {
              console.log(resp);
              this.$message.success("添加成功!");
            } else {
              this.$message.error("添加失败!");
            }
          })
        } else {
          this.$message.error("请填写完整信息");
          return false;
        }
      });
    },
    //用户登出
    submitLogout() {
      this.getAnonymousRequest('/logout').then(resp => {
        if (resp.data.code === 200) {
          localStorage.removeItem('jwtToken');
          this.$router.replace("/");
          this.$message.success(resp.data.data);
        }
      }).catch(error => {
        console.error('无响应:', error);
        this.$message.error('登出失败');
      });
    },
    //添加费用记录
    submitBillingRecord() {
      this.$refs.billingRecordForm.validate((valid) => {
        if (valid) {
          this.postRequest("/addBillingRecord", this.billingRecord).then((resp) => {
            if (resp.data.code === 200) {
              this.$message.success("添加成功！");
              this.addBillingRecordDialogVisible = false;
            } else {
              this.$message.error(resp.data.msg);
            }
          });
        } else {
          this.$message.error("请填写完整信息");
        }
      });
    },
    //添加用药记录
    submitMedicationRecord() {
      this.$refs.medicationRecordForm.validate((valid) => {
        if (valid) {
          this.postRequest("/addMedicationRecord", this.medicationRecord).then((resp) => {
            if (resp.data.code === 200) {
              this.$message.success("添加成功！");
              this.addMedicationRecordDialogVisible = false;
            } else {
              this.$message.error(resp.data.msg);
            }
          });
        } else {
          this.$message.error("请填写完整信息");
        }
      });
    },
    // 提交添加病历的表单
    submitAddMedicalRecord() {
      this.$refs.addMedicalRecordForm.validate((valid) => {
        if (valid) {
          // 发送添加病历的请求
          this.postRequest('/addMedicalRecord', this.medicalRecord).then((resp) => {
            if (resp.data.code === 200) {
              this.$message.success('添加病历成功！');
              this.addMedicalRecordDialogVisible = false;
            } else {
              this.$message.error(resp.data.msg);
            }
          });
        } else {
          this.$message.error('请填写完整信息');
        }
      });
    },
    //删除患者
    deleteUser(id) {
      this.deleteRequest(`/patient/delete/${id}`).then(resp => {
        if (resp.data.code === 200) {
          this.$message.success("删除成功!");
          this.getAllUsers(false);
        } else if (resp.data.code === 400) {
          this.$message.error(resp.data.data);
        }
      }).catch(error => {
        console.error('无响应:', error);
        this.$message.error('删除失败');
      });
    },
    //修改患者
    editUser(user) {
      if (user.editing) {
        this.putRequest('/patient/user', user).then(resp => {
          if (resp.data.code === 200) {
            this.$message.success("修改成功!");
            user.editing = false;
          } else {
            this.$message.error("修改失败!");
          }
        });
      } else {
        user.editing = true;
      }
    },
    //查询患者
    searchUser() {
      if (this.searchQuery === '') {
        this.$message.warning("请输入患者ID或姓名");
        return;
      }
      const isId = /^\d+$/.test(this.searchQuery);
      const queryParam = isId ? 'patientId' : 'name';
      this.getRequest(`/patient/user?${queryParam}=${this.searchQuery}`).then(resp => {
        if (resp.data.code === 200) {
          this.patients = Array.isArray(resp.data.data) ? resp.data.data.map(user => ({ ...user, editing: false })) : [{ ...resp.data.data, editing: false }];
          this.$message.success("查询成功!");
        } else {
          this.$message.error(resp.data.msg);
        }
      }).catch(error => {
        console.error('无响应:', error);
        if (error.response) {
          console.error('服务器错误:', error.response.data);
          this.$message.error('查询失败: ' + error.response.data.message);
        } else {
          this.$message.error('查询失败');
        }
      });
    },
    //更新用户状态
    updateUserStatus(userId, prop, value) {
      const status = value === 'true';
      const otherProp = prop === 'status' ? 'disable' : 'status'; // 获取另一个按钮的属性名
      const otherValue = this.users.find(user => user.userId === userId)[otherProp]; // 获取另一个按钮的当前状态

      this.putRequest(`/sysUser/status`, { userId, [prop]: status, [otherProp]: otherValue === 'true' }).then(resp => {
        if (resp.data.code === 200) {
          this.$message.success("状态更新成功!");
        } else {
          this.$message.error("状态更新失败!");
        }
      }).catch(error => {
        console.error('无响应:', error);
        this.$message.error('状态更新失败');
      });
    },
    //获取登录用户基本信息
    fetchCurrentUser() {
      const userId = localStorage.getItem('userId');
      this.getRequest(`/sysUser/user?userId=${userId}`).then(resp => {
        if (resp.data.code === 200) {
          this.currentUser = resp.data.data;
        } else {
          this.$message.error("获取用户信息失败!");
        }
      }).catch(error => {
        console.error('无响应:', error);
        this.$message.error('获取用户信息失败');
      });
    },
    //登出处理
    handleCommand(command) {
      if (command === 'logout') {
        this.submitLogout();
      }
    },
    //患者信息分页处理
    //每页记录数处理
    handleSizeChange(val) {
      this.pageSize = val;
      this.getAllUsers(false);
    },
    //页码处理
    handleCurrentChange(val) {
      this.currentPage = val;
      this.getAllUsers(false);
    },
    // 病历信息分页处理
    handleMedicalRecordCurrentChange(val) {
      this.medicalCurrentPage = val;
      this.queryMedicalRecord(this.selectedPatientId);
    },
    handleMedicalRecordSizeChange(val) {
      this.medicalPageSize = val;
      this.queryMedicalRecord(this.selectedPatientId);
    }
  }
}
</script>

<style scoped>
.form-item-border {
  border: 1px solid #dcdfe6;
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 10px;
}
.page-background {
  display: flex; /*设置为弹性容器*/
  flex-direction: column; /*主轴设置为垂直方向*/
  justify-content: center; /*容器内元素在主轴上居中*/
  align-items: center; /*容器内元素在交叉轴(与主轴垂直的轴)上居中*/

  background-image: url("@/assets/333.png");
  background-size: 100% 100%; /*图片高宽100%填充容器*/
  background-attachment: fixed; /*固定背景*/

  width: 1500px;
  height: 1500px;
  //padding: 20px;

  .app-main-wrap {
    flex: 1; /* 让子容器占据剩余的可用空间 */
    width: 100%; /*只有单个元素时，想单独更改子容器长度必须同时指定弹性容器主轴长度和子容器最大长度*/
    border-radius: 15px; /*设置4角边框弧度*/
    background: inherit; /*继承父容器背景*/

    box-shadow: inset 0 0 0 3000px rgba(255, 255, 255, 0.3); /*box-shadow: rgba(0, 0, 0, 0.1) 0px 15px 30px;*/
    backdrop-filter: blur(10px);
    padding: 30px 20px;

    .title-wrap {
      height: 80px;
      line-height: 80px; /* 行高 */
      font-size: 20px; /* 字体大小 */
      font-weight: bold; /* 字体加粗 */

      .title {
        margin: 0; /* 外边距 */
        padding-left: 25px; /* 内边距左侧 */
      }

      .logo {
        position: relative; /* 相对位置 */
        top: -3px; /* 元素的上边界离开其正常位置的偏移 */
        margin-right: 5px; /* 外边距右侧 */
        width: 40px; /* 宽度 */
        vertical-align: middle; /* 行内元素垂直居中对齐 */
      }
    }

    .hg {
      width: 333px;
      height: 400px;
      background-image: url("@/assets/678.png");
      background-size: 100% 100%; /*图片高宽100%填充容器*/
      position: absolute; /*相对于父容器的绝对定位*/
      top: 1px;
      right: 100px;
      border-radius: 5px;
    }
  }

  /* 新增样式 */
  .top-right-dropdown {
    position: absolute;
    top: 20px;
    right: 20px;
  }
}
</style>