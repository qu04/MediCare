import request from './index'
import type { OperationLog, Result, SecurityOverview } from '../types'

export function getSecurityOverview() {
  return request.get<any, Result<SecurityOverview>>('/system/security-overview')
}

export function listOperationLogs(params?: {
  module?: string
  status?: number
  keyword?: string
  limit?: number
}) {
  return request.get<any, Result<OperationLog[]>>('/system/operation-logs', { params })
}
