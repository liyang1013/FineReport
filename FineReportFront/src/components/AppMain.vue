<template>
    <router-view v-slot="{ Component }">
        <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
        </transition>
    </router-view>
</template>

<script lang="ts" setup>
</script>

<style lang="scss" scoped>
.fade-transform-leave-active,
.fade-transform-enter-active {
    transition: all 0.5s;
}

.fade-transform-enter-from {
    opacity: 0;
    transform: translateX(-30px);
}

.fade-transform-leave-to {
    opacity: 0;
    transform: translateX(30px);
}
</style>