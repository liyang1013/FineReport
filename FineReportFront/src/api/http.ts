import axios, { type AxiosInstance, type AxiosResponse, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

const service: AxiosInstance = axios.create({
  baseURL: 'http://localhost:3000',
  timeout: 1000 * 60,
  responseType: 'json',
})

service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.warning(res.message)
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  (error) => {
    ElMessage.error(error)
    return Promise.reject(error)
  },
)

function request<T>(config: AxiosRequestConfig): Promise<T> {
  return service.request(config).then((res) => res.data)
}

export default request
