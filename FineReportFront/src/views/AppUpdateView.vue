<template>
    <div class="app-container">
        <el-card class="table-card">
            <el-table :data="tableData" border stripe v-loading="loading" style="width: 100%"
                height="calc(100vh - 100px)">
                <el-table-column prop="version" label="版本">
                    <template #default="{ row }">
                        {{ `V${row.version}` }}
                    </template>
                </el-table-column>
                <el-table-column prop="updateMessage" label="更新信息" />
                <el-table-column prop="downloadUrl" label="下载连接">
                    <template #default="{ row }">
                        <a :href="`/static/${row.downloadUrl}`" target="_blank">{{ row.downloadUrl }}</a>
                    </template>
                </el-table-column>
                <el-table-column prop="forceUpdate" label="是否必须升级">
                    <template #default="{ row }">
                        {{ row.forceUpdate ? '是' : '否' }}
                    </template>
                </el-table-column>
            </el-table>
        </el-card>
    </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { getAppList } from '@/api/app'
import type { AppInfo } from '@/api/types'

const tableData = ref<AppInfo[]>([]);
const loading = ref(false);
const handleSearch = async () => {
    loading.value = true;
    try {
        const res = await getAppList()
        tableData.value = res;
    } catch (error) {
        console.error('Error fetching device list:', error);
        ElMessage.error('获取App列表失败');
    } finally {
        loading.value = false;
    }
};

onMounted(() => {
    handleSearch();
});
</script>

<style scoped lang="scss">
.app-container {
    // padding: 10px;
    // background-color: #f5f7fa;
    // width: 100vw;
    // min-height: 100vh;
    // box-sizing: border-box;

    .search-card {
        margin-bottom: 20px;
        border-radius: 12px;
        box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);

        :deep(.el-card__body) {
            padding: 18px 20px;

            .el-form-item {
                margin-bottom: 10px;
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
        width: 160px;
    }

    .drawer-header {
        padding: 10px;
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
        padding: 10px;
        border-top: 1px solid #e8e8e8;
        text-align: right;
    }
}
</style>