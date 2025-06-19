export interface DeviceInfo {
  deviceId: string
  ipAddress: string
  url: string
  lastSeen: string
  remark: string
}

export interface SearchParams {
  deviceId?: string
  ipAddress?: string
  url?: string
  remark?: string
}
