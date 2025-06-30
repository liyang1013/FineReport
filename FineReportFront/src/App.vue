<template>
  <div class="app-wrapper">
    <el-container>
      <el-aside :width="sidebarWidth">
        <sidebar :is-collapse="isCollapse" />
      </el-aside>

      <el-container>
        <el-header>
          <navbar @toggle-collapse="toggleCollapse" :is-collapse="isCollapse" />
        </el-header>

        <el-main>
          <app-main />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import Sidebar from '@/components/Sidebar.vue'
import Navbar from '@/components/Navbar.vue'
import AppMain from '@/components/AppMain.vue'

const isCollapse = ref(false)

const sidebarWidth = computed(() => {
  return isCollapse.value ? '64px' : '160px'
})

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}
</script>

<style lang="scss" scoped>
.app-wrapper {
  height: 100vh;
  width: 100vw;
  overflow: hidden;

  .el-container {
    height: 100%;
  }

  .el-header {
    background-color: #fff;
    box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
    display: flex;
    align-items: center;
    padding: 0 20px;
    height: 50px;
  }

  .el-aside {
    background-color: #001529;
    color: #fff;
    transition: width 0.3s;
    height: 100vh;
  }

  .el-main {
    background-color: #f0f2f5;
    padding: 20px;
    height: calc(100vh - 60px);
    overflow: hidden;
  }
}
</style>
