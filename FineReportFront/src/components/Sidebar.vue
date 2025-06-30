<template>
    <el-scrollbar wrap-class="scrollbar-wrapper">
        <el-menu :default-active="activeMenu" background-color="#001529" text-color="#bfcbd9"
            active-text-color="#409EFF" :collapse="isCollapse" :unique-opened="false" mode="vertical">
            <sidebar-item v-for="route in routes" :key="route.path" :item="route" :base-path="route.path"
                :is-collapse="isCollapse" />
        </el-menu>
    </el-scrollbar>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SidebarItem from '@/components/SidebarItem.vue'
import type { RouteItem } from '@/api/types'

defineProps({
    isCollapse: {
        type: Boolean,
        default: false
    }
})


const route = useRoute()

const routes: RouteItem[] = [
    {
        path: '/deviceInfo',
        name: 'DeviceInfo',
        meta: { title: '看板管理', icon: 'DataAnalysis' }
    },
    {
        path: '/appUpdate',
        name: 'AppUpdate',
        meta: { title: 'App列表', icon: 'Files' }
    }

]

const activeMenu = computed(() => {
    const { path } = route
    return path
})
</script>

<style lang="scss" scoped>
.el-scrollbar {
    height: 100%;

    .scrollbar-wrapper {
        overflow-x: hidden !important;
    }

    .el-menu {
        border-right: none;
        height: 100%;
    }
}
</style>