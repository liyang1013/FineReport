export interface DeviceInfo {
  deviceId: string
  ipAddress: string
  url: string
  lastSeen: string
  remark: string
  position: string
  department: string
  name: string
  type: string
  isUpdate: boolean
}

export const defaultDeviceInfo: Partial<DeviceInfo> = {
  isUpdate: true,
}

export interface SearchParams {
  deviceId?: string
  ipAddress?: string
  url?: string
  remark?: string
  position?: string
  department?: string
  name?: string
  type?: string
}
