<template>
    <el-menu-item :index="basePath" @click="openRoute(basePath)">
        <el-icon>
            <component :is="item.meta.icon" />
        </el-icon>
        <template #title>
            <span>{{ item.meta.title }}</span>
        </template>
    </el-menu-item>
</template>

<script lang="ts" setup>

import type { RouteItem } from '@/api/types'
import { useRouter } from 'vue-router';

interface Props {
    item: RouteItem
    basePath: string
    isCollapse: boolean
}

const props = defineProps<Props>()


const router = useRouter();
const openRoute = (basePath: string) => {
    router.push(basePath);
}
</script>