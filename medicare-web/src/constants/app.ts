export type AppRole = 'admin' | 'doctor' | 'pharmacist'

export interface AppMenuItem {
  path: string
  title: string
  icon: string
  description: string
  roles: AppRole[]
}

export const APP_NAME = 'MediCare Cloud'
export const APP_SUBTITLE = '智慧门诊运营管理平台'
export const APP_DESCRIPTION = '面向门诊、药房与基础运营的一体化医疗协同系统'

export const ROLE_LABELS: Record<AppRole, string> = {
  admin: '系统管理员',
  doctor: '门诊医生',
  pharmacist: '药剂师',
}

export const ROLE_BADGE_TYPES: Record<AppRole, 'danger' | 'primary' | 'success'> = {
  admin: 'danger',
  doctor: 'primary',
  pharmacist: 'success',
}

export const APP_MENU_ITEMS: AppMenuItem[] = [
  {
    path: '/dashboard',
    title: '运营总览',
    icon: 'HomeFilled',
    description: '查看今日门诊态势、库存风险与运营提醒',
    roles: ['admin', 'doctor', 'pharmacist'],
  },
  {
    path: '/patients',
    title: '患者管理',
    icon: 'User',
    description: '维护患者档案、过敏信息与就诊基础资料',
    roles: ['admin', 'doctor'],
  },
  {
    path: '/basic-data',
    title: '基础数据',
    icon: 'Folder',
    description: '维护科室、医生、排班等主数据',
    roles: ['admin', 'doctor'],
  },
  {
    path: '/registration',
    title: '挂号预约',
    icon: 'Calendar',
    description: '处理预约挂号与前台分诊',
    roles: ['admin'],
  },
  {
    path: '/workstation',
    title: '医生工作站',
    icon: 'FirstAidKit',
    description: '面向门诊医生的接诊与诊疗工作台',
    roles: ['admin', 'doctor'],
  },
  {
    path: '/medical-records',
    title: '病历管理',
    icon: 'Document',
    description: '查看和维护电子病历记录',
    roles: ['admin', 'doctor'],
  },
  {
    path: '/pharmacy',
    title: '药品库存',
    icon: 'Box',
    description: '维护库存、批次与安全库存预警',
    roles: ['admin', 'doctor', 'pharmacist'],
  },
  {
    path: '/prescriptions',
    title: '处方管理',
    icon: 'Notebook',
    description: '查看处方流转、发药与作废状态',
    roles: ['admin', 'doctor', 'pharmacist'],
  },
  {
    path: '/settings',
    title: '系统设置',
    icon: 'Setting',
    description: '维护账户、权限与密码安全策略',
    roles: ['admin', 'doctor', 'pharmacist'],
  },
]

export const DASHBOARD_SHORTCUTS = [
  { title: '新建患者档案', path: '/patients', icon: 'User', roles: ['admin', 'doctor'] as AppRole[] },
  { title: '维护科室排班', path: '/basic-data', icon: 'Calendar', roles: ['admin', 'doctor'] as AppRole[] },
  { title: '处理门诊挂号', path: '/registration', icon: 'Tickets', roles: ['admin'] as AppRole[] },
  { title: '查看库存预警', path: '/pharmacy', icon: 'WarningFilled', roles: ['admin', 'doctor', 'pharmacist'] as AppRole[] },
]
