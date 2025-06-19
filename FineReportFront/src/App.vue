<template>
  <div class="container">
    <el-card class="search-card">
      <el-form :model="searchForm" label-position="right" label-width="65px" :inline="true" label-suffix=":">

        <el-form-item label="设备ID">
          <el-input v-model="searchForm.deviceId" placeholder="请输入设备ID" style="width: 200px;" clearable />
        </el-form-item>

        <el-form-item label="IP地址">
          <el-input v-model="searchForm.ipAddress" placeholder="请输入IP地址" style="width: 200px;" clearable />
        </el-form-item>

        <el-form-item label="URL">
          <el-input v-model="searchForm.url" placeholder="请输入URL" style="width: 200px;" clearable />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="searchForm.remark" placeholder="请输入备注" style="width: 200px;" clearable />
        </el-form-item>

        <el-form-item style="float:right;">
          <el-button type="primary" @click="handleSearch" round>查询</el-button>
          <el-dropdown @command="handleCommand" style="margin-left: 10px;">
            <el-button type="success" round>
              批量操作<el-icon>
                <ArrowDown />
              </el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="url_update">推送url</el-dropdown-item>
                <el-dropdown-item command="clear_webView">清空看板</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </el-form-item>

      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading" @selection-change="handleSelectionChange"
        style=" overflow-y: auto; overflow-x: hidden; height: calc(100vh - 135px); max-height: calc(100vh -135px);">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="deviceId" label="设备ID" />
        <el-table-column prop="ipAddress" label="IP地址" />
        <el-table-column prop="url" label="URL" />
        <el-table-column prop="remark" label="备注" />
        <el-table-column prop="lastSeen" label="最近访问时间" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" @click="handleEdit(row)" :icon="Edit" circle />
            <el-button type="danger" :icon="Delete" circle @click="handleDelete(row)" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { getDeviceList, deleteDevice, sendInfo, updateDevice } from './api/device'
import type { DeviceInfo, SearchParams } from './api/types'
import { Edit, Delete } from '@element-plus/icons-vue'
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
  } finally {
    loading.value = false;
  }
};


const handleCommand = async (command: string) => {
  if (multipleSelection.value.length == 0) { ElMessage.warning("请先选择要操作得设备"); return }
  const deviceIdList: string[] = multipleSelection.value.map(obj => obj.deviceId);
  await sendInfo(deviceIdList, command);
};

const handleEdit = (row: DeviceInfo) => {
  ElMessageBox.prompt("请输入新的URL", "编辑", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    inputValue: row.url,
  })
    .then(async ({ value }) => {
      row.url = value;
      await updateDevice(row)
      ElMessage.success("修改成功");

    })
    .catch(() => {
      ElMessage.info("取消编辑");
    });
};

const multipleSelection = ref<DeviceInfo[]>([])
const handleSelectionChange = (val: DeviceInfo[]) => {
  multipleSelection.value = val
}

const handleDelete = (row: DeviceInfo) => {
  ElMessageBox.confirm("确定要删除这条记录吗?", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      await deleteDevice(row.deviceId);
      ElMessage.success("删除成功");
      handleSearch()
    })
    .catch(() => {
      ElMessage.info("取消删除");
    });
};


onMounted(() => {
  handleSearch();
});
</script>

<style scoped lang="scss">
.container {
  padding: 20px;
  background: rgb(221 221 221);
  width: 100vw;
  height: 100vh;
  overflow: hidden;

  :deep(.search-card) {
    margin-bottom: 20px;
    height: 53px;
    border-radius: 15px;

    .el-card__body {
      padding: 10px 0;

      .el-form-item {
        margin-bottom: 0;
      }
    }
  }

  .table-card {
    margin-bottom: 20px;
    height: calc(100vh - 110px);
    max-height: calc(100vh - 110px);
    border-radius: 15px;

    :deep(.el-card__body) {
      padding: 10px;
    }

  }
}
</style>