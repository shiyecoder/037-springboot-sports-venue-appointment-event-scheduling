// 全局格式化工具：全系统统一调用，禁止各页面自己拼时间
export function fmtTime(v) {
  if (!v) return '—'
  const s = String(v).replace('T', ' ')
  return s.length >= 19 ? s.slice(0, 19) : s
}

export function fmtDate(v) {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 10)
}

export function fmtShort(v) {
  if (!v) return '—'
  const s = String(v).replace('T', ' ')
  return s.slice(5, 16)
}

export function fmtMoney(v) {
  if (v === null || v === undefined || v === '') return '¥0.00'
  return '¥' + Number(v).toFixed(2)
}

export function fmtPct(v) {
  if (v === null || v === undefined) return '0%'
  return Number(v).toFixed(1) + '%'
}

/** 业务状态 → 中文文案 + 标签配色，全系统统一 */
export const STATUS = {
  // 预约状态机
  PENDING_APPROVAL: ['待审批', 't-amber'],
  PENDING_CONFIRM: ['待场地确认', 't-amber'],
  PENDING_ENTRY: ['待入场', 't-blue'],
  IN_PROGRESS: ['使用中', 't-green'],
  COMPLETED: ['已完成', 't-green'],
  CANCELLED: ['已取消', 't-gray'],
  REJECTED: ['已驳回', 't-red'],
  NO_SHOW: ['未到违约', 't-red'],
  WAITLIST: ['候补中', 't-amber'],
  // 通用
  IN_USE: ['使用中', 't-green'],
  LEAVING: ['待离场检查', 't-blue'],
  VIOLATED: ['已违约', 't-red'],
  RESCHEDULE: ['建议调整', 't-amber'],
  WAITING: ['候补中', 't-amber'],
  EXPIRED: ['已过期', 't-gray'],
  CONFIRMED: ['已确认', 't-green'],
  REVOKED: ['已撤销', 't-gray'],
  PAID: ['已支付', 't-green'],
  UNPAID: ['未支付', 't-amber'],
  DRAFT: ['草稿', 't-gray'],
  PUBLISHED: ['已发布', 't-blue'],
  ONGOING: ['进行中', 't-green'],
  FINISHED: ['已结束', 't-gray'],
  NORMAL: ['正常', 't-green'],
  MAINTAINING: ['维护中', 't-amber'],
  DISABLED: ['停用', 't-gray'],
  FULL: ['已满', 't-red'],
  FREE: ['可预约', 't-green'],
  LOCKED: ['锁定', 't-gray'],
  PENDING: ['待处理', 't-amber'],
  APPROVED: ['已通过', 't-green'],
  PASSED: ['已通过', 't-green'],
  APPEALING: ['申诉中', 't-amber'],
  BORROWED: ['借用中', 't-blue'],
  RETURNED: ['已归还', 't-green'],
  DAMAGED: ['损坏待赔', 't-red'],
  OVERDUE: ['超期', 't-red'],
  RECOMMEND: ['建议通过', 't-green'],
  COORDINATE: ['需协调', 't-amber'],
  REJECT: ['建议驳回', 't-red'],
  PENDING_APPROVAL_X: ['待审批', 't-amber']
}

export function statusText(code) {
  const hit = STATUS[code]
  return hit ? hit[0] : (code || '—')
}

export function statusClass(code) {
  const hit = STATUS[code]
  return hit ? hit[1] : 't-gray'
}

/** 预约类型、赛事等级等字典文案 */
export const BOOKING_TYPE = { PERSONAL: '个人预约', TEAM: '团体预约', EVENT: '赛事占用' }
export const FEE_STATUS = { UNPAID: '未支付', PAID: '已支付', REFUNDED: '已退款', FREE: '免费' }
