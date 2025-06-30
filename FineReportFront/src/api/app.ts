import request from '@/api/http'
import type { AppInfo } from '@/api/types'

// 获取设备列表
export function getAppList() {
  return request<AppInfo[]>({
    url: '/api/app/getAppList',
    method: 'post',
  })
}
