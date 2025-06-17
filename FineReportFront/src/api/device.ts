import request from './http'
import type { DeviceInfo, SearchParams } from './types'

// 获取设备列表
export function getDeviceList(params: SearchParams) {
  console.log(params)
  return request<DeviceInfo>({
    url: '/api/devices/query',
    method: 'get',
  })
}

// 更新设备信息
export function updateDevice(id: number, data: Partial<DeviceInfo>) {
  return request<DeviceInfo>({
    url: `/devices/${id}`,
    method: 'post',
    data,
  })
}

// 删除设备
export function deleteDevice(id: number) {
  return request<boolean>({
    url: `/devices/${id}`,
    method: 'delete',
  })
}
