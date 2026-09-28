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
        <!-- 患者ID/姓名查询搜索框 -->
        <el-row v-if="activeTab === 'first'">
          <el-col :span="12">
            <el-input v-if="hasGetPermission" v-model="searchQuery" placeholder="患者ID/姓名/身份证号" clearable style="width: 200px;" @keydown.enter.native="searchUser"></el-input>
            <el-button v-if="hasGetPermission" type="primary" @click="searchUser">查询</el-button>
          </el-col>
        </el-row>

        <!-- 身份证号查询搜索框 -->
        <el-row v-if="activeTab === 'five'">
          <el-col :span="12">
            <el-input v-if="hasGetPermission" v-model="idCardNumber" placeholder="请输入身份证号" clearable style="width: 200px;" />
            <el-button v-if="hasGetPermission" type="primary" @click="searchByIdCardNumber">查询</el-button>
          </el-col>
        </el-row>

        <el-tabs v-model="activeTab">
          <!-- 患者信息标签页 -->
          <el-tab-pane v-if=hasDoctorRole label="患者信息" name="first">
            <el-button v-if="hasGetPermission" type="primary" icon="el-icon-refresh-right" @click="getAllUsers(true)">刷新</el-button>
            <div style="margin-top: 20px;"></div>

            <!-- 滚动容器：超过3条自动出现滚动条 -->
            <div
                :style="{
      maxHeight: patients.length > 3 ? '400px' : 'auto',
      overflowY: patients.length > 3 ? 'auto' : 'hidden',
      marginBottom: '15px'
    }"
            >
              <el-table :data="patients" style="width: 100%; border-radius: 5px;">
                <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label" :width="col.width" align="center">
                  <template slot-scope="scope">
                    <!-- 出生日期下拉选择框 -->
                    <template v-if="scope.row.editing && col.prop === 'birthDate'">
                      <el-select v-model="scope.row.year" placeholder="年" style="width: 80px;" @change="handleBirthDateChange(scope.row)">
                        <el-option v-for="year in years" :key="year" :label="year" :value="year"></el-option>
                      </el-select>
                      <el-select v-model="scope.row.month" placeholder="月" style="width: 70px;" @change="handleBirthDateChange(scope.row)">
                        <el-option v-for="month in months" :key="month" :label="month" :value="month"></el-option>
                      </el-select>
                      <el-select v-model="scope.row.day" placeholder="日" style="width: 70px;" @change="handleBirthDateChange(scope.row)">
                        <el-option v-for="day in getDaysInMonth(scope.row.year, scope.row.month)" :key="day" :label="day" :value="day"></el-option>
                      </el-select>
                    </template>
                    <!-- 年龄字段禁用输入，仅展示 -->
                    <el-input v-else-if="scope.row.editing && col.prop === 'age'" v-model="scope.row[col.prop]" disabled style="width: 100%;"></el-input>
                    <el-input v-else-if="scope.row.editing" v-model="scope.row[col.prop]"></el-input>
                    <template v-else>
                      <span v-if="col.prop === 'birthDate'">{{ formatDate(scope.row.birthDate) }}</span>
                      <span v-else>{{ scope.row[col.prop] }}</span>
                    </template>
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
            </div>
            <!-- 滚动容器结束 -->

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

          <!-- 用户信息标签页 -->
          <el-tab-pane v-if=hasAdminRole label="用户信息" name="second">
            <div style="margin-top: 20px;"></div>
            <el-row :gutter="20">
              <el-col :span="6" v-for="(user) in users" :key="user.userId">
                <div style="margin-bottom: 20px;"></div>
                <!-- 卡片设置相对定位，用于右上角图标定位 -->
                <el-card class="box-card" style="position: relative;">
                  <!-- 锁按钮在卡牌右上角，纯图标，悬浮提示解锁/锁定 -->
                  <div style="position: absolute; top: 10px; right: 15px; z-index: 9;">
                    <el-button :class="['lock-btn',user.status === 'false' ? 'lock-closed' : 'lock-opened']"
                    style="background:transparent; border:none; padding:0;"
                    @click="updateUserStatus(user.userId, 'status', user.status === 'true' ? 'false' : 'true')"
                    :title="user.status === 'true' ? '锁定' : '解锁'"
                    >
                    <i
                        :class="user.status === 'true' ? 'el-icon-unlock' : 'el-icon-lock'"
                        class="big-lock-icon"
                    ></i>
                    </el-button>
                  </div>

                  <div slot="header" class="clearfix">
                    <span>{{ user.name }}</span>
                  </div>
                  <div v-for="col in userColumns" :key="col.prop" class="text item">
                    <template v-if="col.prop === 'status'">
                    </template>

                    <!-- ====================== 是否启用 el-switch 开关 ====================== -->
                    <template v-else-if="col.prop === 'disable'">
                      <span>{{ col.label }}:</span>
                      <el-switch
                          v-model="user.disable"
                          active-value="true"
                          inactive-value="false"
                          active-color="#67c23a"
                          inactive-color="#f56c6c"
                          style="margin-left: 10px;"
                          @change="updateUserStatus(user.userId, 'disable', user.disable)"
                      >
                      </el-switch>
                    </template>
                    <!-- ====================== 开关结束 ====================== -->
                    <template v-else>
                      <span>{{ col.label }}: {{ user[col.prop] }}</span>
                    </template>
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

          <!-- 添加患者标签页 -->
          <el-tab-pane v-if=hasDoctorRole label="添加患者" name="third">
            <el-form label-width="80px" :model="addUserParams" :rules="rules" ref="addUserForm">
              <el-form-item v-for="item in formItems" :key="item.prop" :label="item.label" :prop="item.prop">
                <el-input v-if="item.type === 'input' && item.prop !== 'birthDate' && item.prop !== 'age'" v-model="addUserParams[item.prop]" :placeholder="item.placeholder" clearable style="width: 200px;"></el-input>
                <!-- 年龄字段禁用输入，仅展示 -->
                <el-input v-if="item.prop === 'age'" v-model="addUserParams.age" disabled placeholder="自动计算" style="width: 200px;"></el-input>
                <!-- 添加患者的出生日期下拉选择框 -->
                <template v-if="item.prop === 'birthDate'">
                  <el-select v-model="addUserParams.year" placeholder="年" style="width: 80px;" @change="handleAddBirthDateChange">
                    <el-option v-for="year in years" :key="year" :label="year" :value="year"></el-option>
                  </el-select>
                  <el-select v-model="addUserParams.month" placeholder="月" style="width: 70px;" @change="handleAddBirthDateChange">
                    <el-option v-for="month in months" :key="month" :label="month" :value="month"></el-option>
                  </el-select>
                  <el-select v-model="addUserParams.day" placeholder="日" style="width: 70px;" @change="handleAddBirthDateChange">
                    <el-option v-for="day in getDaysInMonth(addUserParams.year, addUserParams.month)" :key="day" :label="day" :value="day"></el-option>
                  </el-select>
                </template>
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

          <!-- 角色管理标签页 -->
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

          <!-- 指定患者信息标签页 -->
          <el-tab-pane v-if="hasPatientRole" label="指定患者信息" name="five">
            <el-table :data="selectedPatient" style="width: 100%; border-radius: 5px;">
              <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label" :width="col.width" align="center">
                <template slot-scope="scope">
                  <span v-if="col.prop === 'birthDate'">{{ formatDate(scope.row.birthDate) }}</span>
                  <span v-else>{{ scope.row[col.prop] }}</span>
                </template>
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
          <!-- 展示昵称、账户、角色、登出 -->
          <el-dropdown-item command="name">
            用户昵称：{{ currentUser.name }}
          </el-dropdown-item>
          <el-dropdown-item command="account">
            账户：{{ currentUser.account }}
          </el-dropdown-item>
          <el-dropdown-item command="roleNames">
            用户角色：{{ currentUser.roleNames }}
          </el-dropdown-item>
          <el-dropdown-item divided command="logout">
            登出
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>

    <!-- 病历信息弹窗 -->
    <el-dialog
        title="病历信息"
        :visible.sync="dialogVisible"
        width="50%"
        @close="resetMedicalDialog"
    >
      <!-- 所有按钮放在这里（不截图） -->
      <div v-for="(record, index) in medicalRecord" :key="index" style="margin-bottom:10px;">
        <el-button v-if="hasGetPermission" type="success" icon="el-icon-money" @click="openBillingRecordDialog(record.recordId)">费用查询</el-button>

        <!-- 费用添加按钮：已有记录 → 灰色实心、禁用 -->
        <el-button
            v-if="hasSetPermission"
            :class="{ 'disabled-gray-btn': hasBillingRecord }"
            :disabled="hasBillingRecord"
            icon="el-icon-plus"
            @click="openAddBillingRecordDialog(record.recordId)"
        >
          {{ hasBillingRecord ? '已添加费用' : '添加费用信息' }}
        </el-button>

        <el-button v-if="hasGetPermission" type="success" icon="el-icon-money" @click="openMedicationRecordDialog(record.recordId)">用药查询</el-button>

        <!-- 用药添加按钮：已有记录 → 灰色实心、禁用 -->
        <el-button
            v-if="hasSetPermission"
            :class="{ 'disabled-gray-btn': hasMedicationRecord }"
            :disabled="hasMedicationRecord"
            icon="el-icon-plus"
            @click="openAddMedicationRecordDialog(record.recordId)"
        >
          {{ hasMedicationRecord ? '已添加用药' : '添加用药信息' }}
        </el-button>

        <el-button v-if="hasGetPermission" type="primary" icon="el-icon-download" @click="downloadMedicalImage">下载病历</el-button>
      </div>

      <!-- 只有病历内容，独立ref，只截图这里 -->
      <div ref="medicalContent" style="padding: 10px 0;">
        <el-form label-width="120px">
          <el-form-item
              v-for="(record, index) in medicalRecord"
              :key="index"
              class="form-item-border"
          >
            <el-form-item
                v-for="col in medicalColumns"
                :key="col.prop"
                :label="col.label"
                :prop="col.prop"
                class="form-item-border"
            >
              {{ formatDate(medicalRecord[index][col.prop]) }}
            </el-form-item>
          </el-form-item>
        </el-form>
      </div>

      <!-- 病历分页 -->
      <el-pagination
          style="margin-top:15px;text-align:center"
          @size-change="handleMedicalRecordSizeChange"
          @current-change="handleMedicalRecordCurrentChange"
          :current-page="medicalCurrentPage"
          :page-sizes="[1, 2, 3, 4, 5]"
          :page-size="medicalPageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="totalRecords"
      ></el-pagination>
    </el-dialog>

    <!-- 添加病历信息弹窗 - 年月日下拉框 -->
    <el-dialog
        title="添加病历"
        :visible.sync="addMedicalRecordDialogVisible"
        width="60%"
        @close="resetAddMedicalRecordDialog"
        @opened="onAddMedicalRecordDialogOpened"
    >
      <el-form label-width="120px" :model="medicalRecord" :rules="addMedicalRecordRules" ref="addMedicalRecordForm">
        <!-- 患者ID字段 -->
        <el-form-item label="患者ID" prop="patientId">
          <el-input v-model="medicalRecord.patientId" disabled></el-input>
        </el-form-item>
        <!-- 就诊日期 - 年月日下拉框 -->
        <el-form-item label="就诊日期" prop="visitDate" required>
          <div style="display: flex; gap: 10px;">
            <el-select v-model="tempVisitDate.year" placeholder="年" style="width: 100px;" @change="handleVisitDateChange">
              <el-option v-for="year in dateYears" :key="year" :label="year" :value="year"></el-option>
            </el-select>
            <el-select v-model="tempVisitDate.month" placeholder="月" style="width: 80px;" @change="handleVisitDateChange">
              <el-option v-for="month in months" :key="month" :label="month" :value="month"></el-option>
            </el-select>
            <el-select v-model="tempVisitDate.day" placeholder="日" style="width: 80px;" @change="handleVisitDateChange">
              <el-option v-for="day in getDaysInMonth(tempVisitDate.year, tempVisitDate.month)" :key="day" :label="day" :value="day"></el-option>
            </el-select>
          </div>
        </el-form-item>
        <!-- 主诉 -->
        <el-form-item label="主诉" prop="chiefComplaint">
          <el-input v-model="medicalRecord.chiefComplaint" type="textarea" :rows="2"></el-input>
        </el-form-item>
        <!-- 现病史 -->
        <el-form-item label="现病史" prop="presentIllness">
          <el-input v-model="medicalRecord.presentIllness" type="textarea" :rows="2"></el-input>
        </el-form-item>
        <!-- 既往史 -->
        <el-form-item label="既往史" prop="pastHistory">
          <el-input v-model="medicalRecord.pastHistory" type="textarea" :rows="2"></el-input>
        </el-form-item>
        <!-- 家族史 -->
        <el-form-item label="家族史" prop="familyHistory">
          <el-input v-model="medicalRecord.familyHistory" type="textarea" :rows="2"></el-input>
        </el-form-item>
        <!-- 体格检查 -->
        <el-form-item label="体格检查" prop="physicalExamination">
          <el-input v-model="medicalRecord.physicalExamination" type="textarea" :rows="2"></el-input>
        </el-form-item>
        <!-- 诊断 -->
        <el-form-item label="诊断" prop="diagnosis">
          <el-input v-model="medicalRecord.diagnosis" type="textarea" :rows="2"></el-input>
        </el-form-item>
        <!-- 治疗方案 -->
        <el-form-item label="治疗方案" prop="treatmentPlan">
          <el-input v-model="medicalRecord.treatmentPlan" type="textarea" :rows="2"></el-input>
        </el-form-item>
        <!-- 出院日期 - 年月日下拉框 -->
        <el-form-item label="出院日期" prop="dischargeDate">
          <div style="display: flex; gap: 10px;">
            <el-select v-model="tempDischargeDate.year" placeholder="年" style="width: 100px;" @change="handleDischargeDateChange">
              <el-option v-for="year in dateYears" :key="year" :label="year" :value="year"></el-option>
            </el-select>
            <el-select v-model="tempDischargeDate.month" placeholder="月" style="width: 80px;" @change="handleDischargeDateChange">
              <el-option v-for="month in months" :key="month" :label="month" :value="month"></el-option>
            </el-select>
            <el-select v-model="tempDischargeDate.day" placeholder="日" style="width: 80px;" @change="handleDischargeDateChange">
              <el-option v-for="day in getDaysInMonth(tempDischargeDate.year, tempDischargeDate.month)" :key="day" :label="day" :value="day"></el-option>
            </el-select>
          </div>
        </el-form-item>
        <!-- 出院诊断 -->
        <el-form-item label="出院诊断" prop="dischargeDiagnosis">
          <el-input v-model="medicalRecord.dischargeDiagnosis" type="textarea" :rows="2"></el-input>
        </el-form-item>
        <!-- 出院医嘱 -->
        <el-form-item label="出院医嘱" prop="dischargeInstructions">
          <el-input v-model="medicalRecord.dischargeInstructions" type="textarea" :rows="2"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitAddMedicalRecord">提交</el-button>
          <el-button @click="addMedicalRecordDialogVisible = false">取消</el-button>
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

      <!-- 下载按钮 -->
      <div style="margin-bottom:10px;">
        <el-button v-if="hasGetPermission" type="primary" icon="el-icon-download" @click="downloadBillingImage">下载费用单</el-button>
      </div>

      <!-- 费用内容，加 ref -->
      <div ref="billingContent" style="padding: 10px 0;">
        <el-form label-width="120px">
          <el-form-item v-for="(col, index) in billingColumns" :key="index">
            <el-form-item :label="col.label" :prop="col.prop" class="form-item-border">
              {{ billingRecord[col.prop] }}
            </el-form-item>
          </el-form-item>
        </el-form>
      </div>
    </el-dialog>

    <!-- 费用信息添加弹窗 -->
    <el-dialog
        title="费用信息添加"
        :visible.sync="addBillingRecordDialogVisible"
        width="50%"
        @close="resetAddBillingRecordDialog"
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

      <!-- 下载按钮 -->
      <div style="margin-bottom:10px;">
        <el-button v-if="hasGetPermission" type="primary" icon="el-icon-download" @click="downloadMedicationImage">下载用药单</el-button>
      </div>

      <!-- 用药内容，加 ref -->
      <div ref="medicationContent" style="padding: 10px 0;">
        <el-form label-width="120px">
          <el-form-item v-for="(col, index) in medicationColumns" :key="index">
            <el-form-item :label="col.label" :prop="col.prop" class="form-item-border">
              {{ formatDate(medicationRecord[col.prop]) }}
            </el-form-item>
          </el-form-item>
        </el-form>
      </div>
    </el-dialog>

    <!-- 添加用药信息弹窗 - 修改为年月日下拉框 -->
    <el-dialog
        title="添加用药信息"
        :visible.sync="addMedicationRecordDialogVisible"
        width="50%"
        @close="resetAddMedicationRecordDialog"
        @opened="onAddMedicationRecordDialogOpened"
    >
      <el-form label-width="120px" :model="medicationRecord" :rules="medicationRecordRules" ref="medicationRecordForm">
        <!-- 药品名称 -->
        <el-form-item label="药品名称" prop="drugName">
          <el-input v-model="medicationRecord.drugName"></el-input>
        </el-form-item>
        <!-- 剂量 -->
        <el-form-item label="剂量" prop="dosage">
          <el-input v-model="medicationRecord.dosage"></el-input>
        </el-form-item>
        <!-- 用药频率 -->
        <el-form-item label="用药频率" prop="frequency">
          <el-input v-model="medicationRecord.frequency"></el-input>
        </el-form-item>
        <!-- 开始日期 - 年月日下拉框 -->
        <el-form-item label="开始日期" prop="startDate" required>
          <div style="display: flex; gap: 10px;">
            <el-select v-model="tempStartDate.year" placeholder="年" style="width: 100px;" @change="handleStartDateChange">
              <el-option v-for="year in dateYears" :key="year" :label="year" :value="year"></el-option>
            </el-select>
            <el-select v-model="tempStartDate.month" placeholder="月" style="width: 80px;" @change="handleStartDateChange">
              <el-option v-for="month in months" :key="month" :label="month" :value="month"></el-option>
            </el-select>
            <el-select v-model="tempStartDate.day" placeholder="日" style="width: 80px;" @change="handleStartDateChange">
              <el-option v-for="day in getDaysInMonth(tempStartDate.year, tempStartDate.month)" :key="day" :label="day" :value="day"></el-option>
            </el-select>
          </div>
        </el-form-item>
        <!-- 结束日期 - 年月日下拉框 -->
        <el-form-item label="结束日期" prop="endDate" required>
          <div style="display: flex; gap: 10px;">
            <el-select v-model="tempEndDate.year" placeholder="年" style="width: 100px;" @change="handleEndDateChange">
              <el-option v-for="year in dateYears" :key="year" :label="year" :value="year"></el-option>
            </el-select>
            <el-select v-model="tempEndDate.month" placeholder="月" style="width: 80px;" @change="handleEndDateChange">
              <el-option v-for="month in months" :key="month" :label="month" :value="month"></el-option>
            </el-select>
            <el-select v-model="tempEndDate.day" placeholder="日" style="width: 80px;" @change="handleEndDateChange">
              <el-option v-for="day in getDaysInMonth(tempEndDate.year, tempEndDate.month)" :key="day" :label="day" :value="day"></el-option>
            </el-select>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitMedicationRecord">提交</el-button>
          <el-button @click="addMedicationRecordDialogVisible = false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

  </div>
</template>

<script>
import html2canvas from 'html2canvas';

export default {
  name: "PatientInfo",
  data() {
    const backendHost = window.location.hostname;
    return {
      roles: [], // 所有角色列表
      allAuthorities: [], // 所有权限列表
      patients: [],
      selectedPatient:[], //身份证指定患者存储
      users: [],
      addUserParams: {
        patientId: null,
        name: null,
        gender: null,
        birthDate: null,
        age: null,
        idCardNumber: null,
        contactNumber: null,
        address: null,
        year: null,  // 年份
        month: null, // 月份
        day: null    // 日期
      },
      options: [{ value: '男', label: '男' }, { value: '女', label: '女' }],
      searchQuery: '',
      idCardNumber:'',
      // 患者信息表格列
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

      // 添加病历弹窗临时日期变量
      tempVisitDate: { year: null, month: null, day: null },
      tempDischargeDate: { year: null, month: null, day: null },
      // 添加用药弹窗临时日期变量
      tempStartDate: { year: null, month: null, day: null },
      tempEndDate: { year: null, month: null, day: null },

      // 病历展示列
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
      // 费用展示列
      billingColumns: [
        { prop: 'itemName', label: '费用项目名称：' },
        { prop: 'itemCost', label: '费用金额：' },
        { prop: 'paymentMethod', label: '支付方式：' },
        { prop: 'insuranceInfo', label: '保险信息：' }
      ],
      // 用药展示列
      medicationColumns:[
        { prop: 'drugName', label: '药品名称：' },
        { prop: 'dosage', label: '剂量：' },
        { prop: 'frequency', label: '用药频率：' },
        { prop: 'startDate', label: '开始日期：' },
        { prop: 'endDate', label: '结束日期：' }
      ],
      // 去除是否正常显示，仅占位
      userColumns: [
        { prop: 'userId', label: '用户ID', width: '70px' },
        { prop: 'name', label: '用户昵称', width: '80px' },
        { prop: 'account', label: '账号', width: '100px' },
        { prop: 'status', label: '状态', width: '0px' },
        { prop: 'disable', label: '是否启用', width: '80px' }
      ],
      //添加患者信息表格列
      formItems: [
        { prop: 'name', label: '姓名', type: 'input', placeholder: '请输入内容' },
        { prop: 'gender', label: '性别', type: 'select', placeholder: '请选择' },
        { prop: 'birthDate', label: '出生日期', type: 'input', placeholder: '请输入内容' },
        { prop: 'age', label: '年龄', type: 'input', placeholder: '自动计算' },
        { prop: 'idCardNumber', label: '身份证号', type: 'input', placeholder: '请输入内容' },
        { prop: 'contactNumber', label: '联系电话', type: 'input', placeholder: '请输入内容' },
        { prop: 'address', label: '地址', type: 'input', placeholder: '请输入内容' }
      ],
      // 年份下拉选项（1900-当前年份）
      years: Array.from({length: new Date().getFullYear() - 1900 + 1}, (_, i) => 1900 + i),
      // 用于病历和用药的年份范围（1900-当前年份+5，允许未来日期）
      dateYears: Array.from({length: new Date().getFullYear() - 1900 + 6}, (_, i) => 1900 + i),
      // 月份下拉选项
      months: Array.from({length: 12}, (_, i) => i + 1),
      // 添加患者表单校验规则
      rules: {
        name: [
          { required: true, message: '请输入姓名', trigger: 'blur' },
          { validator: (rule, value, callback) => {
              const val = (value || '').trim();
              if (/^\d+$/.test(val)) {
                callback(new Error('姓名不能是纯数字！'));
              } else {
                callback();
              }
            }, trigger: 'blur' }
        ],
        gender: [
          { required: true, message: '请选择性别', trigger: 'change' }
        ],
        birthDate: [
          { required: true, message: '请选择出生日期', trigger: 'change' }
        ],
        age: [
          { required: false }
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
      // 费用记录校验规则
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
      // 用药记录校验规则
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
          { required: true, message: '请选择开始日期', trigger: 'change' }
        ],
        endDate: [
          { required: true, message: '请选择结束日期', trigger: 'change' }
        ]
      },
      // 添加病历校验规则
      addMedicalRecordRules: {
        patientId: [
          { required: true, message: '请输入患者 ID', trigger: 'blur' }
        ],
        visitDate: [
          { required: true, message: '请选择就诊日期', trigger: 'change' }
        ],
        chiefComplaint: [
          { required: true, message: '请输入主诉', trigger: 'blur' }
        ],
        presentIllness: [
          { required: true, message: '请输入现病史', trigger: 'blur' }
        ],
        pastHistory: [
          { required: true, message: '请输入既往史', trigger: 'blur' }
        ],
        familyHistory: [
          { required: true, message: '请输入家族史', trigger: 'blur' }
        ],
        physicalExamination: [
          { required: true, message: '请输入体格检查结果', trigger: 'blur' }
        ],
        diagnosis: [
          { required: true, message: '请输入诊断结果', trigger: 'blur' }
        ],
        treatmentPlan: [
          { required: true, message: '请输入治疗方案', trigger: 'blur' }
        ],
        dischargeDate: [
          { required: false, message: '请选择出院日期', trigger: 'change' }
        ],
        dischargeDiagnosis: [
          { required: true, message: '请输入出院诊断结果', trigger: 'blur' }
        ],
        dischargeInstructions: [
          { required: true, message: '请输入出院医嘱', trigger: 'blur' }
        ]
      },
      activeTab: 'first',
      currentUser: [],
      currentPage: 1,
      pageSize: 10,
      total: 0,

      // 标记是否已有费用/用药记录，用于按钮禁用
      hasBillingRecord: false,
      hasMedicationRecord: false,

      totalRecords: 0, // 病历总记录数
      medicalCurrentPage: 1,  // 当前病历页数
      medicalPageSize: 1,    // 每页病历条数
      // WebSocket 实例
      ws: null,
      // 后端WebSocket地址（根据实际部署修改）
      wsUrl: process.env.NODE_ENV === 'development'
          ? `ws://${backendHost}:8082/ws/`
          : `ws://${backendHost}:8082/ws/`//生产地址
    }
  },
  mounted() {
    // 统一获取token
    const token = sessionStorage.getItem('jwtToken') || localStorage.getItem('jwtToken');
    if (!token) {
      this.$message.warning('请先登录！');
      this.$router.replace('/');
      return;
    }

    this.getAllUsers(false);
    this.getAllSysUsers(false);
    this.fetchCurrentUser();
    this.getRoles(); // 获取所有角色
    this.getAllAuthorities(); // 获取所有权限
    // 建立WebSocket连接
    this.initWebSocket();
  },
  computed: {
    // 获取用户角色
    userRoles() {
      return JSON.parse(sessionStorage.getItem("userRoles") || localStorage.getItem("userRoles") || '[]');
    },
    // 获取用户权限
    userAuthorities() {
      return JSON.parse(sessionStorage.getItem("userAuthorities") || localStorage.getItem("userAuthorities") || '[]');
    },
    // 查询权限判断
    hasGetPermission() {
      return this.userAuthorities.includes('resource:get')|| this.userAuthorities.includes('all');
    },
    // 编辑权限判断
    hasSetPermission() {
      return this.userAuthorities.includes('resource:set')|| this.userAuthorities.includes('all');
    },
    // 删除权限判断
    hasRemovePermission() {
      return this.userAuthorities.includes('resource:remove')|| this.userAuthorities.includes('all');
    },
    // 管理员角色判断
    hasAdminRole() {
      return this.userRoles.includes('admin')|| this.userRoles.includes('test 1');
    },
    // 医生角色判断
    hasDoctorRole() {
      return this.userRoles.includes('doctor')|| this.userRoles.includes('test 1');
    },
    // 患者角色判断
    hasPatientRole() {
      return this.userRoles.includes('patient')|| this.userRoles.includes('test 1');
    }
  },
  beforeDestroy() {
    // 页面销毁关闭WebSocket
    this.closeWebSocket();
  },
  methods: {
    // WebSocket初始化
    initWebSocket() {
      // 统一获取userId
      const userId = this.getUserId();
      if (!userId) {
        this.$message.warning('用户未登录，无法建立实时连接');
        return;
      }

      // 创建WebSocket连接
      this.ws = new WebSocket(this.wsUrl + userId);

      // 连接成功
      this.ws.onopen = () => {
        console.log('WebSocket 权限实时连接已建立');
      };

      // 收到后端推送消息
      this.ws.onmessage = (event) => {
        const msg = event.data;
        console.log('收到消息：', msg);

        // 收到强制下线指令
        if (msg === 'forceLogout') {
          this.$message.warning('账号已被管理员禁用，即将强制下线！');
          localStorage.clear();
          sessionStorage.clear();

          // 延迟跳转到登录页
          setTimeout(() => {
            this.$router.replace('/');
            window.location.reload();
          }, 1500);
          return;
        }

        // 收到权限刷新指令
        if (msg === 'refreshPermissions') {
          this.refreshUserPermissions();
        }

        // 处理用户状态变更推送
        if (msg.startsWith('register:')) {
          const account = msg.split(':')[1];
          this.$message.success(`新用户注册：${account}`);
          this.getAllSysUsers(true); // 刷新管理员用户列表
        }

        if (msg.startsWith('lock:')) {
          const account = msg.split(':')[1];
          this.$message.warning(`用户已锁定：${account}`);
          this.getAllSysUsers(true); // 刷新管理员用户列表
        }

        if (msg.startsWith('unlock:')) {
          const account = msg.split(':')[1];
          this.$message.success(`用户已解锁：${account}`);
          this.getAllSysUsers(true); // 刷新管理员用户列表
        }
      };

      // 连接错误
      this.ws.onerror = (error) => {
        console.error('WebSocket 连接失败：', error);
      };

      // 连接关闭
      this.ws.onclose = () => {
        console.log('WebSocket 连接已断开');
      };
    },

    // 关闭WebSocket
    closeWebSocket() {
      if (this.ws) {
        this.ws.close();
        this.ws = null;
      }
    },

    // 刷新用户权限
    refreshUserPermissions() {
      // 统一获取userId
      const userId = this.getUserId();
      if (!userId) return;

      // 1. 重新获取最新角色和权限
      this.getRequest(`/sysUser/RoleAndAndAuthority?userId=${userId}`).then(resp => {
        if (resp.data.code === 200) {
          const data = resp.data.data;
          const storage = sessionStorage.getItem('jwtToken') ? sessionStorage : localStorage;
          // 2. 覆盖本地存储的角色和权限
          storage.setItem('userRoles', JSON.stringify(data.role));
          storage.setItem('userAuthorities', JSON.stringify(data.authority));

          // 3. 提示用户权限已更新
          this.$message.success('权限已更新，页面已自动刷新！');

          // 4. 重新加载当前页面，使新权限立即生效
          setTimeout(() => {
            window.location.reload();
          }, 800);
        }
      }).catch(() => {
        this.$message.error('权限更新失败，请手动刷新页面');
      });
    },

    // 日期格式化：将带时间的字符串转为 YYYY-MM-DD
    formatDate(date) {
      if (!date) return '';
      // 如果已经是 YYYY-MM-DD 格式，直接返回
      if (/^\d{4}-\d{2}-\d{2}$/.test(date)) return date;
      try {
        const d = new Date(date);
        if (isNaN(d.getTime())) return date;
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
      } catch (e) {
        return date;
      }
    },
    // 根据出生日期计算年龄
    calculateAge(birthYear, birthMonth, birthDay) {
      if (!birthYear || !birthMonth || !birthDay) return null;

      const birthDate = new Date(birthYear, birthMonth - 1, birthDay);
      const today = new Date();
      let age = today.getFullYear() - birthDate.getFullYear();

      // 计算月份差，如果还没到生日，年龄减1
      const monthDiff = today.getMonth() - birthDate.getMonth();
      if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
        age--;
      }

      return age;
    },
    // 获取某年某月的天数
    getDaysInMonth(year, month) {
      if (!year || !month) return [];
      const daysInMonth = new Date(year, month, 0).getDate();
      return Array.from({length: daysInMonth}, (_, i) => i + 1);
    },
    // 兼容旧版getDays方法（使用当前年份）
    getDays(month) {
      if (!month) return [];
      const year = new Date().getFullYear();
      return this.getDaysInMonth(year, month);
    },
    // 编辑患者时出生日期变化，更新年龄
    handleBirthDateChange(row) {
      // 更新出生日期
      if (row.year && row.month && row.day) {
        row.birthDate = `${row.year}-${String(row.month).padStart(2, '0')}-${String(row.day).padStart(2, '0')}`;
        // 计算年龄
        row.age = this.calculateAge(row.year, row.month, row.day);
      }
    },
    // 添加患者时出生日期变化，更新年龄，并触发表单验证
    handleAddBirthDateChange() {
      // 更新出生日期
      if (this.addUserParams.year && this.addUserParams.month && this.addUserParams.day) {
        this.addUserParams.birthDate = `${this.addUserParams.year}-${String(this.addUserParams.month).padStart(2, '0')}-${String(this.addUserParams.day).padStart(2, '0')}`;
        // 计算年龄
        this.addUserParams.age = this.calculateAge(this.addUserParams.year, this.addUserParams.month, this.addUserParams.day);
      }
      // 手动触发表单验证，清除出生日期的错误提示
      this.$nextTick(() => {
        if (this.$refs.addUserForm) {
          this.$refs.addUserForm.clearValidate('birthDate');
          this.$refs.addUserForm.validateField('birthDate');
        }
      });
    },
    // 病历就诊日期变化处理
    handleVisitDateChange() {
      if (this.tempVisitDate.year && this.tempVisitDate.month && this.tempVisitDate.day) {
        this.medicalRecord.visitDate = `${this.tempVisitDate.year}-${String(this.tempVisitDate.month).padStart(2, '0')}-${String(this.tempVisitDate.day).padStart(2, '0')}`;
      } else {
        this.medicalRecord.visitDate = '';
      }
      this.$nextTick(() => {
        if (this.$refs.addMedicalRecordForm) {
          this.$refs.addMedicalRecordForm.clearValidate('visitDate');
          this.$refs.addMedicalRecordForm.validateField('visitDate');
        }
      });
    },
    // 病历出院日期变化处理
    handleDischargeDateChange() {
      if (this.tempDischargeDate.year && this.tempDischargeDate.month && this.tempDischargeDate.day) {
        this.medicalRecord.dischargeDate = `${this.tempDischargeDate.year}-${String(this.tempDischargeDate.month).padStart(2, '0')}-${String(this.tempDischargeDate.day).padStart(2, '0')}`;
      } else {
        this.medicalRecord.dischargeDate = '';
      }
      this.$nextTick(() => {
        if (this.$refs.addMedicalRecordForm) {
          this.$refs.addMedicalRecordForm.clearValidate('dischargeDate');
          this.$refs.addMedicalRecordForm.validateField('dischargeDate');
        }
      });
    },
    // 用药开始日期变化处理
    handleStartDateChange() {
      if (this.tempStartDate.year && this.tempStartDate.month && this.tempStartDate.day) {
        this.medicationRecord.startDate = `${this.tempStartDate.year}-${String(this.tempStartDate.month).padStart(2, '0')}-${String(this.tempStartDate.day).padStart(2, '0')}`;
      } else {
        this.medicationRecord.startDate = '';
      }
      this.$nextTick(() => {
        if (this.$refs.medicationRecordForm) {
          this.$refs.medicationRecordForm.clearValidate('startDate');
          this.$refs.medicationRecordForm.validateField('startDate');
        }
      });
    },
    // 用药结束日期变化处理
    handleEndDateChange() {
      if (this.tempEndDate.year && this.tempEndDate.month && this.tempEndDate.day) {
        this.medicationRecord.endDate = `${this.tempEndDate.year}-${String(this.tempEndDate.month).padStart(2, '0')}-${String(this.tempEndDate.day).padStart(2, '0')}`;
      } else {
        this.medicationRecord.endDate = '';
      }
      this.$nextTick(() => {
        if (this.$refs.medicationRecordForm) {
          this.$refs.medicationRecordForm.clearValidate('endDate');
          this.$refs.medicationRecordForm.validateField('endDate');
        }
      });
    },
    // 重置添加病历弹窗数据
    resetAddMedicalRecordDialog() {
      this.medicalRecord = {
        patientId: this.selectedPatientId || null,
        visitDate: '',
        dischargeDate: ''
      };
      this.tempVisitDate = { year: null, month: null, day: null };
      this.tempDischargeDate = { year: null, month: null, day: null };
      this.$nextTick(() => {
        if (this.$refs.addMedicalRecordForm) {
          this.$refs.addMedicalRecordForm.clearValidate();
        }
      });
    },
    // 关闭病历弹窗时重置所有状态
    resetMedicalDialog() {
      this.dialogVisible = false;
      // 清空病历数据
      this.medicalRecord = [];
      // 清空选中患者ID
      this.selectedPatientId = null;
      // 重置病历分页为第一页
      this.medicalCurrentPage = 1;
      // 重置总记录数
      this.totalRecords = 0;
      // 重置费用/用药记录状态
      this.hasBillingRecord = false;
      this.hasMedicationRecord = false;
    },
    // 重置添加用药弹窗数据
    resetAddMedicationRecordDialog() {
      this.medicationRecord = {
        medicationId: null,
        recordId: this.currentRecordId || null,
        startDate: '',
        endDate: ''
      };
      this.tempStartDate = { year: null, month: null, day: null };
      this.tempEndDate = { year: null, month: null, day: null };
      this.$nextTick(() => {
        if (this.$refs.medicationRecordForm) {
          this.$refs.medicationRecordForm.clearValidate();
        }
      });
    },
    // 重置添加费用弹窗数据
    resetAddBillingRecordDialog() {
      this.billingRecord = {
        recordId: this.currentRecordId || null,
      };
      this.$nextTick(() => {
        if (this.$refs.billingRecordForm) {
          this.$refs.billingRecordForm.clearValidate();
        }
      });
    },
    // 弹窗打开时的回调
    onAddMedicalRecordDialogOpened() {
      this.$nextTick(() => {
        if (this.$refs.addMedicalRecordForm) {
          this.$refs.addMedicalRecordForm.clearValidate();
        }
      });
    },
    onAddMedicationRecordDialogOpened() {
      this.$nextTick(() => {
        if (this.$refs.medicationRecordForm) {
          this.$refs.medicationRecordForm.clearValidate();
        }
      });
    },
    // 身份证查询患者
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
    },
    // 打开添加病历弹窗
    openAddMedicalRecordDialog(patientId) {
      this.selectedPatientId = patientId;
      this.medicalRecord = {
        patientId: patientId,
        visitDate: '',
        dischargeDate: ''
      };
      this.tempVisitDate = { year: null, month: null, day: null };
      this.tempDischargeDate = { year: null, month: null, day: null };
      this.addMedicalRecordDialogVisible = true;
    },
    // 打开费用信息弹窗
    openBillingRecordDialog(recordId) {
      this.queryBillingRecord(recordId); // 查询费用信息
    },
    // 打开添加费用信息弹窗
    openAddBillingRecordDialog(recordId) {
      this.currentRecordId = recordId;
      this.billingRecord = {
        recordId: recordId,
      };
      this.addBillingRecordDialogVisible = true;
    },
    // 关闭费用信息弹窗
    closeBillingRecordDialog() {
      this.billingDialogVisible = false; // 关闭费用弹窗
    },
    // 查询费用记录
    queryBillingRecord(recordId) {
      this.getRequest(`/BillingRecord?recordId=${recordId}`).then((resp) => {
        if (resp.data.code === 200) {
          this.billingRecord = resp.data.data;
          // 有数据才打开
          this.billingDialogVisible = true;
        } else {
          this.$message.error("未找到指定费用记录");
          this.billingDialogVisible = false;
        }
      }).catch((error) => {
        console.error('查询费用记录失败:', error);
        this.$message.error('查询费用记录失败');
        this.billingDialogVisible = false;
      });
    },
    // 打开用药信息弹窗
    openMedicationRecordDialog(recordId) {
      this.queryMedicationRecord(recordId); // 查询费用信息
    },
    // 打开添加用药信息弹窗
    openAddMedicationRecordDialog(recordId) {
      this.currentRecordId = recordId;
      this.medicationRecord = {
        recordId: recordId,
        startDate: '',
        endDate: ''
      };
      this.tempStartDate = { year: null, month: null, day: null };
      this.tempEndDate = { year: null, month: null, day: null };
      this.addMedicationRecordDialogVisible = true;
    },
    // 关闭用药信息弹窗
    closeMedicationRecordDialog() {
      this.medicationDialogVisible = false; // 关闭费用弹窗
    },
    // 查询用药记录
    queryMedicationRecord(recordId) {
      this.getRequest(`/MedicationRecord?recordId=${recordId}`).then((resp) => {
        if (resp.data.code === 200) {
          this.medicationRecord = resp.data.data;
          // 有数据才打开
          this.medicationDialogVisible = true;
        } else {
          this.$message.error("未找到指定用药记录");
          this.medicationDialogVisible = false;
        }
      }).catch((error) => {
        console.error('查询用药记录失败:', error);
        this.$message.error('查询用药记录失败');
        this.medicationDialogVisible = false;
      });
    },
    // 分页查询病历信息
    queryMedicalRecord(patientId) {
      this.getRequest(`/MedicalRecordPage?current=${this.medicalCurrentPage}&size=${this.medicalPageSize}&patientId=${patientId}`).then(resp => {
        if (resp.data.code === 200) {
          this.medicalRecord = resp.data.data.records;
          this.totalRecords = resp.data.data.total;

          // 如果有病历，检查是否已有费用/用药记录
          if (this.medicalRecord.length > 0) {
            const recordId = this.medicalRecord[0].recordId;
            // 查询费用单是否存在
            this.getRequest(`/BillingRecord?recordId=${recordId}`).then(res => {
              this.hasBillingRecord = res.data.code === 200;
            }).catch(() => this.hasBillingRecord = false);

            // 查询用药单是否存在
            this.getRequest(`/MedicationRecord?recordId=${recordId}`).then(res => {
              this.hasMedicationRecord = res.data.code === 200;
            }).catch(() => this.hasMedicationRecord = false);
          } else {
            this.hasBillingRecord = false;
            this.hasMedicationRecord = false;
          }
          // =====================================================

          this.dialogVisible = true;
        } else {
          this.$message.error("未找到指定病历");
          this.dialogVisible = false;
        }
      }).catch(error => {
        console.error('查询病历失败:', error);
        this.$message.error('查询病历失败');
        this.dialogVisible = false;
      });
    },
    //分页获取所有患者信息
    getAllUsers(showMessage = true) {
      this.getRequest(`/patient/AllUsersPage?current=${this.currentPage}&size=${this.pageSize}`).then(resp => {
        if (resp.data.code === 200) {
          if (showMessage) this.$message.success("刷新成功!");
          // 解析出生日期为年月日，并计算年龄
          this.patients = resp.data.data.records.map(patient => {
            const birthDate = patient.birthDate;
            let year, month, day, age;
            if (birthDate) {
              const dateParts = birthDate.split('-');
              year = parseInt(dateParts[0]);
              month = parseInt(dateParts[1]);
              day = parseInt(dateParts[2]);
              // 计算年龄
              age = this.calculateAge(year, month, day);
            }
            return {
              ...patient,
              editing: false,
              year,
              month,
              day,
              age: age || patient.age // 使用计算的年龄
            };
          });
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
            this.$message.error("检测到有角色无权限!");
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
            this.$message.error("检测到有用户无角色!");
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
          this.handleAddBirthDateChange();
          this.postRequest("/patient/user", this.addUserParams).then(resp => {
            if (resp.data.code === 200) {
              console.log(resp);
              this.$message.success("添加成功!");
              // 刷新患者列表
              this.getAllUsers(false);
              // 重置表单
              this.addUserParams = {
                patientId: null,
                name: null,
                gender: null,
                birthDate: null,
                age: null,
                idCardNumber: null,
                contactNumber: null,
                address: null,
                year: null,
                month: null,
                day: null
              };
            } else {
              // 直接显示后端返回的错误信息
              this.$message.error(resp.data.msg || "添加失败!");
            }
          }).catch(err => {// eslint-disable-line no-unused-vars
            //还没写处理网路异常的代码，留待以后
            this.$message.error("网络异常，添加失败");
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
          // 彻底清空
          localStorage.clear();
          sessionStorage.clear();
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
      // 先确保日期已设置
      if (this.tempStartDate.year && this.tempStartDate.month && this.tempStartDate.day) {
        this.medicationRecord.startDate = `${this.tempStartDate.year}-${String(this.tempStartDate.month).padStart(2, '0')}-${String(this.tempStartDate.day).padStart(2, '0')}`;
      }
      if (this.tempEndDate.year && this.tempEndDate.month && this.tempEndDate.day) {
        this.medicationRecord.endDate = `${this.tempEndDate.year}-${String(this.tempEndDate.month).padStart(2, '0')}-${String(this.tempEndDate.day).padStart(2, '0')}`;
      }

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
      // 先确保日期已设置
      if (this.tempVisitDate.year && this.tempVisitDate.month && this.tempVisitDate.day) {
        this.medicalRecord.visitDate = `${this.tempVisitDate.year}-${String(this.tempVisitDate.month).padStart(2, '0')}-${String(this.tempVisitDate.day).padStart(2, '0')}`;
      }
      if (this.tempDischargeDate.year && this.tempDischargeDate.month && this.tempDischargeDate.day) {
        this.medicalRecord.dischargeDate = `${this.tempDischargeDate.year}-${String(this.tempDischargeDate.month).padStart(2, '0')}-${String(this.tempDischargeDate.day).padStart(2, '0')}`;
      }

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
        // 确保出生日期和年龄已正确设置
        this.handleBirthDateChange(user);
        this.putRequest('/patient/user', user).then(resp => {
          if (resp.data.code === 200) {
            this.$message.success("修改成功!");
            user.editing = false;
          } else {
            // 直接展示后端返回的错误（重复身份证会在这里显示）
            this.$message.error(resp.data.msg || "修改失败!");
          }
        }).catch(() => {
          this.$message.error("网络异常，修改失败");
        });
      } else {
        user.editing = true;
      }
    },
    // 查询患者（兼容写好的ID/姓名 + 新增身份证，无任何校验，测试用）
    searchUser() {
      if (this.searchQuery === '') {
        this.$message.warning("请输入患者ID/姓名/身份证号");
        return;
      }

      const input = this.searchQuery.trim();

      // 先尝试身份证查询
      this.getRequest(`/patient/idCardNumber?idCardNumber=${input}`).then(res => {
        if (res.data.code === 200) {
          // 身份证查到了 → 展示
          this.patients = Array.isArray(res.data.data) ? res.data.data : [res.data.data];
          this.$message.success("身份证查询成功！");
        } else {
          // 没查到 → 走 ID/姓名 查询
          this.oldSearch(input);
        }
      }).catch(() => {
        // 接口报错 → 走ID/姓名查询
        this.oldSearch(input);
      })
    },

// 查询逻辑（ID/姓名）
    oldSearch(input) {
      const isId = /^\d+$/.test(input);
      const queryParam = isId ? 'patientId' : 'name';
      this.getRequest(`/patient/user?${queryParam}=${input}`).then(resp => {
        if (resp.data.code === 200) {
          const formatPatients = (data) => {
            return Array.isArray(data) ? data.map(patient => {
              const birthDate = patient.birthDate;
              let year, month, day, age;
              if (birthDate) {
                const dateParts = birthDate.split('-');
                year = parseInt(dateParts[0]);
                month = parseInt(dateParts[1]);
                day = parseInt(dateParts[2]);
                age = this.calculateAge(year, month, day);
              }
              return {
                ...patient,
                editing: false,
                year, month, day,
                age: age || patient.age
              };
            }) : [{
              ...data,
              editing: false,
              year: data.birthDate ? parseInt(data.birthDate.split('-')[0]) : null,
              month: data.birthDate ? parseInt(data.birthDate.split('-')[1]) : null,
              day: data.birthDate ? parseInt(data.birthDate.split('-')[2]) : null,
              age: data.birthDate ? this.calculateAge(
                  parseInt(data.birthDate.split('-')[0]),
                  parseInt(data.birthDate.split('-')[1]),
                  parseInt(data.birthDate.split('-')[2])) : data.age
            }];
          };
          this.patients = formatPatients(resp.data.data);
          this.$message.success("查询成功!");
        } else {
          this.$message.error(resp.data.msg);
        }
      })
    },
    // 更新用户状态
    updateUserStatus(userId, prop, value) {
      const status = value === 'true';
      const otherProp = prop === 'status' ? 'disable' : 'status';
      const otherValue = this.users.find(user => user.userId === userId)[otherProp];

      this.putRequest(`/sysUser/updateStatusAndDisable`, {
        userId,
        [prop]: status,
        [otherProp]: otherValue === 'true'
      }).then(resp => {
        if (resp.data.code === 200) {
          // ====================== 这里开始修改提示 ======================
          let msg = "";
          if (prop === "status") {
            // 锁定/解锁 提示
            msg = status ? "用户已解锁" : "用户已锁定";
          } else if (prop === "disable") {
            // 启用/禁用 提示
            msg = status ? "用户已启用" : "用户已禁用";
          }
          this.$message.success(msg);
          // ====================== 修改结束 ======================

          // 前端实时刷新状态
          const user = this.users.find(u => u.userId === userId);
          if (user) {
            user[prop] = value;
          }
          // 禁用时踢人
          if (prop === 'disable' && status === false) {
            this.getRequest(`/ws/kickUser/${userId}`).then(() => {});
          }
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
      // 统一从 session / local 读取 userId
      const userId = this.getUserId();

      if (!userId) {
        this.$message.warning('用户信息异常，请重新登录！');
        localStorage.clear();
        sessionStorage.clear();
        this.$router.replace('/');
        return;
      }

      // 1. 获取用户基本信息
      this.getRequest(`/sysUser/user?userId=${userId}`).then(resp => {
        if (resp.data.code === 200) {
          this.currentUser = resp.data.data;
          // 2. 获取当前用户的角色信息
          this.getRequest(`/sysUser/roles?userId=${userId}`).then(roleResp => {
            if (roleResp.data.code === 200) {
              const roleIds = roleResp.data.data;
              // 3. 匹配角色名称（从已加载的roles列表中找对应描述）
              const roleNames = roleIds.map(roleId => {
                const role = this.roles.find(r => r.roleId === roleId);
                return role ? role.description : `未知角色(${roleId})`;
              }).join('、');
              // 4. 给currentUser添加角色名称字段
              this.currentUser.roleNames = roleNames || '无角色';
            } else {
              this.currentUser.roleNames = '获取角色失败';
            }
          }).catch(error => {
            console.error('获取用户角色失败:', error);
            this.currentUser.roleNames = '获取角色失败';
          });
        } else {
          this.$message.error("获取用户信息失败!");
        }
      }).catch(error => {
        console.error('获取用户信息失败:', error);
        this.$message.error('获取用户信息失败，请重新登录');
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
    },
    // 下载病历图片
    downloadMedicalImage() {
      const dom = this.$refs.medicalContent;
      if (!dom) {
        this.$message.warning('暂无病历内容');
        return;
      }
      html2canvas(dom, { useCORS: true, scale: 2 }).then(canvas => {
        const a = document.createElement('a');
        a.href = canvas.toDataURL('image/png');
        a.download = `病历_${this.selectedPatientId || 'unknown'}.png`;
        a.click();
        this.$message.success('病历下载成功');
      }).catch(() => this.$message.error('下载失败'));
    },

    // 下载费用单图片
    downloadBillingImage() {
      const dom = this.$refs.billingContent;
      if (!dom) {
        this.$message.warning('暂无费用信息');
        return;
      }
      html2canvas(dom, { useCORS: true, scale: 2 }).then(canvas => {
        const a = document.createElement('a');
        a.href = canvas.toDataURL('image/png');
        a.download = `费用单_${this.selectedPatientId || 'unknown'}.png`;
        a.click();
        this.$message.success('费用单下载成功');
      }).catch(() => this.$message.error('下载失败'));
    },

    // 下载用药单图片
    downloadMedicationImage() {
      const dom = this.$refs.medicationContent;
      if (!dom) {
        this.$message.warning('暂无用药信息');
        return;
      }
      html2canvas(dom, { useCORS: true, scale: 2 }).then(canvas => {
        const a = document.createElement('a');
        a.href = canvas.toDataURL('image/png');
        a.download = `用药单_${this.selectedPatientId || 'unknown'}.png`;
        a.click();
        this.$message.success('用药单下载成功');
      }).catch(() => this.$message.error('下载失败'));
    }
  }
}
</script>

<style scoped>
/* 表单项边框样式 */
.form-item-border {
  border: 1px solid #dcdfe6;
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 10px;
}
/* 页面背景样式 */
.page-background {
  display: flex; /*设置为弹性容器*/
  flex-direction: column; /*主轴设置为垂直方向*/
  justify-content: center; /*容器内元素在主轴上居中*/
  align-items: center; /*容器内元素在交叉轴(与主轴垂直的轴)上居中*/

  background-image: url("@/assets/333.png");
  background-size: 100% 100%; /*图片高宽100%填充容器*/
  background-attachment: fixed; /*固定背景*/

  /*容器大小*/
  width: 98vw;
  height: 99vh;

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

  /* 右上角用户下拉菜单样式 */
  .top-right-dropdown {
    position: absolute;
    top: 20px;
    right: 20px;
  }
}
</style>


<style>
/* 灰色禁用按钮样式：实心灰色、不可点击、鼠标禁用 */
.disabled-gray-btn {
background-color: #c0c4cc !important;
border-color: #c0c4cc !important;
color: #fff !important;
cursor: not-allowed !important;
opacity: 1 !important; /* 不透明 */
}
/* 禁用状态 hover 不变色 */
.disabled-gray-btn:hover {
background-color: #c0c4cc !important;
border-color: #c0c4cc !important;
color: #fff !important;
}
</style>


/* ====================== 大锁图标样式 ====================== */
<style>
.big-lock-icon {
  font-size: 40px !important;
  transition: all 0.3s ease;
  cursor: pointer; /* 图标本身也设为手型 */
}
.lock-closed .big-lock-icon {
  color: #f56c6c;
}
.lock-closed:hover .big-lock-icon {
  transform: scale(1.2);
}
.lock-opened .big-lock-icon {
  color: #67c23a;
  opacity: 0.9;
  /* 去掉了 cursor: not-allowed */
  transform: rotate(15deg);
}

/* 强制覆盖 Element UI 禁用按钮的光标样式 */
.lock-btn.el-button[disabled] {
  cursor: pointer !important; /* 永远显示手型 */
  pointer-events: auto !important; /* 确保可点击 */
}

.lock-btn{
  outline: none !important;
  border: none !important;
  background: transparent !important;
  box-shadow: none !important;
  cursor: pointer !important; /* 按钮本身也设为手型 */
}
.lock-btn:focus, .lock-btn:hover{
  background: transparent !important;
  border: none !important;
  outline: none !important;
}
</style>