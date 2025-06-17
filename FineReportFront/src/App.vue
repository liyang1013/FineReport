<template>
  <div class="container">
    <el-card class="search-card">
      <el-form :model="searchForm" label-width="80px" :inline="true" label-suffix=":">

        <el-form-item label="设备ID">
          <el-input v-model="searchForm.deviceId" placeholder="请输入设备ID" style="width: 200px;" clearable />
        </el-form-item>

        <el-form-item label="IP地址">
          <el-input v-model="searchForm.ipAddress" placeholder="请输入IP地址" style="width: 200px;" clearable />
        </el-form-item>

        <el-form-item label="URL">
          <el-input v-model="searchForm.url" placeholder="请输入URL" style="width: 200px;" clearable />
        </el-form-item>

        <el-form-item style="float:right;">
          <el-button type="primary" @click="handleSearch" round>查询</el-button>
          <el-button type="success" @click="handleUpdate" round>更新</el-button>
        </el-form-item>

      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading"
        style=" overflow-y: auto; overflow-x: hidden; height: calc(100vh - 135px); max-height: calc(100vh -135px);">
        <el-table-column prop="deviceId" label="设备ID" />
        <el-table-column prop="ipAddress" label="IP地址" />
        <el-table-column prop="url" label="URL" />
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from "vue";
import type { TableColumnCtx } from "element-plus";
import { ElMessage, ElMessageBox } from "element-plus";
import { getDeviceList, deleteDevice } from './api/device'
import type { DeviceInfo, SearchParams } from './api/types'


// 类型定义
interface SearchForm {
  deviceId: string;
  ipAddress: string;
  url: string;
}

interface TableItem {
  id: number;
  deviceId: string;
  ipAddress: string;
  url: string;
  status: string;
  createTime: string;
}

// 搜索表单
const searchForm = reactive<SearchParams>({
  deviceId: "",
  ipAddress: "",
  url: "",
  remark: "",
});

// 表格数据
const tableData = ref<TableItem[]>([]);
const loading = ref(false);

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

// 初始化数据
const initData = () => {
  // 模拟数据
  const mockData: TableItem[] = [];
  const statuses = ["success", "warning", "danger"];

  for (let i = 1; i <= 50; i++) {
    mockData.push({
      id: i,
      deviceId: `DEV-${1000 + i}`,
      ipAddress: `192.168.1.${i}`,
      url: `https://example.com/api/${i}`,
      status: statuses[i % 3],
      createTime: new Date(Date.now() - i * 3600000).toLocaleString(),
    });
  }

  tableData.value = mockData;
  pagination.total = mockData.length;
};



// 搜索
const handleSearch = async () => {


  const res = await getDeviceList(searchForm)


  // loading.value = true;
  // // 模拟异步搜索
  // setTimeout(() => {
  //   loading.value = false;
  //   ElMessage.success("搜索成功");
  //   // 实际项目中这里应该是调用API
  //   initData();
  // }, 800);
};

// 重置
const handleReset = () => {
  searchForm.deviceId = "";
  searchForm.ipAddress = "";
  searchForm.url = "";
  handleSearch();
};

// 更新
const handleUpdate = () => {
  loading.value = true;
  // 模拟异步更新
  setTimeout(() => {
    loading.value = false;
    ElMessage.success("数据已更新");
    initData();
  }, 1000);
};

// 编辑
const handleEdit = (row: TableItem) => {
  ElMessageBox.prompt("请输入新的URL", "编辑", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    inputValue: row.url,
  })
    .then(({ value }) => {
      row.url = value;
      ElMessage.success("修改成功");
    })
    .catch(() => {
      ElMessage.info("取消编辑");
    });
};

// 删除
const handleDelete = (row: TableItem) => {
  ElMessageBox.confirm("确定要删除这条记录吗?", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(() => {
      tableData.value = tableData.value.filter((item) => item.id !== row.id);
      ElMessage.success("删除成功");
    })
    .catch(() => {
      ElMessage.info("取消删除");
    });
};

// 生命周期钩子
onMounted(() => {

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