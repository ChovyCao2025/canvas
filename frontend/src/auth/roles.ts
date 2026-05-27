import type { UserRole } from '../services/api'

export function isSuperAdminRole(role: UserRole | undefined) {
  return role === 'SUPER_ADMIN' || role === 'ADMIN'
}

export function isTenantAdminRole(role: UserRole | undefined) {
  return role === 'TENANT_ADMIN'
}

export function canManageUsers(role: UserRole | undefined) {
  return isSuperAdminRole(role) || isTenantAdminRole(role)
}

export function canManageTenants(role: UserRole | undefined) {
  return isSuperAdminRole(role)
}

export function canManageSystemOptions(role: UserRole | undefined) {
  return isSuperAdminRole(role) || isTenantAdminRole(role)
}

export function roleLabel(role: UserRole | undefined) {
  switch (role) {
    case 'SUPER_ADMIN':
    case 'ADMIN':
      return '超级管理员'
    case 'TENANT_ADMIN':
      return '租户管理员'
    case 'OPERATOR':
      return '操作员'
    default:
      return '未登录'
  }
}
