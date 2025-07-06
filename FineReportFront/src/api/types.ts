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
  version: string
  centre: string
  isUpdate: boolean
}

export interface AppInfo {
  versionName: string
  versionCode: number
  updateMessage: string
  downloadUrl: string
  forceUpdate: boolean
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
  centre?: string
}

export interface RouteItem {
  path: string
  name: string
  meta: RouteMeta
  children?: RouteItem[]
}

export interface RouteMeta {
  title: string
  icon: string
}

export interface PaginationResponse<T> {
  data: T[]
  total: number
  page: number
  pageSize: number
}
