<template>
  <div class="app-container">
    <el-card class="search-card">
      <el-form :model="searchForm" label-position="right" label-width="80px" :inline="true" label-suffix=":">
        <el-form-item label="设备ID">
          <el-input v-model="searchForm.deviceId" placeholder="请输入设备ID" clearable class="search-input" />
        </el-form-item>

        <el-form-item label="IP地址">
          <el-input v-model="searchForm.ipAddress" placeholder="请输入IP地址" clearable class="search-input" />
        </el-form-item>

        <el-form-item label="URL">
          <el-input v-model="searchForm.url" placeholder="请输入URL" clearable class="search-input" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="searchForm.remark" placeholder="请输入备注" clearable class="search-input" />
        </el-form-item>

        <div class="action-buttons">
          <el-button type="primary" @click="handleSearch" round class="search-button">
            <el-icon>
              <Search />
            </el-icon>
            <span>查询</span>
          </el-button>

          <el-dropdown @command="handleCommand" class="batch-dropdown">
            <el-button type="success" round>
              批量操作<el-icon>
                <ArrowDown />
              </el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="url_update">
                  <el-icon>
                    <Upload />
                  </el-icon>推送URL
                </el-dropdown-item>
                <el-dropdown-item command="clear_webView" divided>
                  <el-icon>
                    <Delete />
                  </el-icon>清空看板
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading" @selection-change="handleSelectionChange"
        style="width: 100%" height="calc(100vh - 180px)">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column prop="deviceId" label="设备ID" width="180" />
        <el-table-column prop="ipAddress" label="IP地址" width="150" />
        <el-table-column prop="url" label="URL" min-width="200" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" width="150" show-overflow-tooltip />
        <el-table-column prop="lastSeen" label="最近访问时间" width="180" />
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="编辑" placement="top">
              <el-button type="primary" @click="showDrawer(row)" :icon="Edit" circle size="small" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button type="danger" :icon="Delete" circle size="small" @click="handleDelete(row)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="drawer" :size="400" :with-header="false">
      <div class="drawer-header">
        <h3>编辑设备信息</h3>
      </div>
      <div class="drawer-content">
        <el-form :model="editRow" label-width="80px" label-suffix=":">
          <el-form-item label="设备ID">
            <el-input v-model="editRow.deviceId" disabled />
          </el-form-item>
          <el-form-item label="IP地址">
            <el-input v-model="editRow.ipAddress" disabled />
          </el-form-item>
          <el-form-item label="URL" required>
            <el-input v-model="editRow.url" placeholder="请输入URL" clearable type="textarea" :rows="6" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="editRow.remark" placeholder="请输入备注" clearable type="textarea" :rows="3" />
          </el-form-item>
        </el-form>
      </div>
      <div class="drawer-footer">
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { getDeviceList, deleteDevice, sendInfo, updateDevice } from './api/device'
import type { DeviceInfo, SearchParams } from './api/types'
import { Edit, Delete, Search, ArrowDown, Upload } from '@element-plus/icons-vue'

const searchForm = reactive<SearchParams>({
  deviceId: "",
  ipAddress: "",
  url: "",
  remark: "",
});

const tableData = ref<DeviceInfo[]>([]);
const loading = ref(false);
const handleSearch = async () => {
  loading.value = true;
  try {
    const res = await getDeviceList(searchForm)
    tableData.value = res;
  } catch (error) {
    console.error('Error fetching device list:', error);
    ElMessage.error('获取设备列表失败');
  } finally {
    loading.value = false;
  }
};

const handleCommand = async (command: string) => {
  if (multipleSelection.value.length == 0) {
    ElMessage.warning("请先选择要操作的设备");
    return;
  }
  try {
    const deviceIdList: string[] = multipleSelection.value.map(obj => obj.deviceId);
    await sendInfo(deviceIdList, command);
    ElMessage.success("操作执行成功");
    handleSearch();
  } catch (error) {
    ElMessage.error("操作执行失败");
  }
};

const drawer = ref(false);
const editRow = ref<DeviceInfo>({
  deviceId: '',
  ipAddress: '',
  url: '',
  remark: '',
  lastSeen: ''
});
const showDrawer = (row: DeviceInfo) => {
  drawer.value = true;
  editRow.value = { ...row };
}

const save = async () => {
  try {
    await updateDevice(editRow.value);
    ElMessage.success("修改成功");
    drawer.value = false;
    handleSearch();
  } catch (error) {
    ElMessage.error("修改失败");
  }
};

const multipleSelection = ref<DeviceInfo[]>([]);
const handleSelectionChange = (val: DeviceInfo[]) => {
  multipleSelection.value = val;
}

const handleDelete = (row: DeviceInfo) => {
  ElMessageBox.confirm("确定要删除这条记录吗?", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
    beforeClose: async (action, instance, done) => {
      if (action === 'confirm') {
        instance.confirmButtonLoading = true;
        try {
          await deleteDevice(row.deviceId);
          ElMessage.success("删除成功");
          handleSearch();
          done();
        } catch (error) {
          ElMessage.error("删除失败");
        } finally {
          instance.confirmButtonLoading = false;
        }
      } else {
        done();
      }
    }
  }).catch(() => {
    ElMessage.info("取消删除");
  });
};

onMounted(() => {
  handleSearch();
});
</script>

<style scoped lang="scss">
.app-container {
  padding: 20px;
  background-color: #f5f7fa;
  width: 100vw;
  min-height: 100vh;
  box-sizing: border-box;

  .search-card {
    margin-bottom: 20px;
    border-radius: 12px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);

    :deep(.el-card__body) {
      padding: 18px 20px;

      .el-form-item {
        margin-bottom: 0;
        margin-right: 15px;
      }

      .action-buttons {
        float: right;
        display: flex;
        align-items: center;

        .search-button {
          padding: 8px 15px;

          .el-icon {
            margin-right: 5px;
          }
        }

        .batch-dropdown {
          margin-left: 10px;
        }
      }
    }
  }

  .table-card {
    border-radius: 12px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);

    :deep(.el-card__body) {
      padding: 0;

      .el-table {
        .el-table__header th {
          background-color: #f8f8f9;
          font-weight: 600;
        }

        .el-button {
          margin: 0 3px;
        }
      }
    }
  }

  .search-input {
    width: 200px;
  }

  .drawer-header {
    padding: 20px;
    border-bottom: 1px solid #e8e8e8;

    h3 {
      margin: 0;
      color: #333;
      font-size: 18px;
    }
  }

  .drawer-content {
    padding: 20px;

    .el-form-item {
      margin-bottom: 22px;
    }
  }

  .drawer-footer {
    padding: 20px;
    border-top: 1px solid #e8e8e8;
    text-align: right;
  }
}
</style>