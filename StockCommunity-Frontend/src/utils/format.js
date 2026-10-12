/**
 * 數字轉千分位字串
 * @param {number|string} value - 要格式化的數值
 * @param {number} decimals - 小數點位數（預設不強製品值）
 * * @param {string} fallback - 當數值無效或為空時的預設回傳值（預設為 '0'）
 */

export const formatNumber = (value, decimals = null, fallback = '0') => {
  // 💡 處理 null、undefined 或空字串，直接回傳自訂的預設值
  if (value === null || value === undefined || value === '') {
    return fallback
  }

  const num = Number(value)
  // 💡 處理無法轉為數字 (NaN) 的情況
  if (isNaN(num)) return fallback

  const options = {}
  if (decimals !== null) {
    options.minimumFractionDigits = decimals
    options.maximumFractionDigits = decimals
  }

  return new Intl.NumberFormat('en-US', options).format(num)
}
