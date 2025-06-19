import request from './http'
import type { DeviceInfo, SearchParams } from './types'

// 获取设备列表
export function getDeviceList(params: SearchParams) {
  return request<DeviceInfo[]>({
    url: '/api/devices/query',
    method: 'post',
    data: params,
  })
}

// 更新设备信息
export function updateDevice(device: DeviceInfo) {
  return request<any>({
    url: '/api/devices/update',
    method: 'post',
    data: device,
  })
}

// 删除设备
export function deleteDevice(device_id: string) {
  return request<any>({
    url: `/api/devices/${device_id}/delete`,
    method: 'get',
  })
}

//批量操作
export function sendInfo(deviceList: string[], type: string) {
  return request<any>({
    url: '/api/devices/ws',
    method: 'post',
    data: { deviceList: deviceList, type: type },
  })
}
