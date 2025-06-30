<template>
    <div class="navbar">
        <div class="left-menu">
            <el-button :icon="isCollapse ? 'Expand' : 'Fold'" circle size="small" @click="$emit('toggle-collapse')" />
        </div>
        <div class="right-menu">
            <el-dropdown :disabled="true" placement="bottom-end">
                <div class="avatar-wrapper">
                    <el-avatar size="small" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" />
                    <span class="username">管理员</span>
                </div>
            </el-dropdown>
        </div>
    </div>
</template>

<script lang="ts" setup>
import { Expand, Fold } from '@element-plus/icons-vue'

defineProps({
    isCollapse: {
        type: Boolean,
        default: false
    }
})

defineEmits(['toggle-collapse'])
</script>

<style lang="scss" scoped>
.navbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
    height: 100%;

    .left-menu {
        display: flex;
        align-items: center;
        padding-left: 0px;

        .el-button {
            margin-right: 16px;
            background-color: #f5f7fa;
            border: none;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);

            &:hover {
                background-color: #e6e8eb;
            }
        }
    }

    .right-menu {
        display: flex;
        align-items: center;
        padding-right: 20px;

        .avatar-wrapper {
            display: flex;
            align-items: center;
            cursor: pointer;
            transition: all 0.3s;

            &:hover {
                background: rgba(0, 0, 0, 0.025);
            }

            .username {
                margin: 0 8px 0 12px;
                font-size: 14px;
                color: #606266;
            }

            .el-icon--right {
                margin-left: 4px;
                color: #909399;
            }
        }
    }
}
</style>