/**
 * 腾讯财经免费实时行情接口封装
 * 接口：https://qt.gtimg.cn/q=sh600519,sz000001
 * 完全免费，无需注册，支持批量查询（逗号分隔）
 *
 * ⚠️ 注意：该接口返回 GBK 编码，必须用 arraybuffer + TextDecoder 解码
 *
 * 返回格式示例：
 *   v_sh600519="1~茅台~600519~1384.79~1401.17~1400.00~52753~20299~32453~...";
 *
 * 字段（以 ~ 分隔，索引从 0 开始）：
 *   [0]  未知（1=A股）
 *   [1]  股票名称（GBK编码，已解码）
 *   [2]  股票代码
 *   [3]  当前价格
 *   [4]  昨收价
 *   [5]  今开价
 *   [6]  成交量（手）
 *   [7]  外盘（手）
 *   [8]  内盘（手）
 *   [9-13]  买一价 ~ 买五价
 *   [14-18] 买一量 ~ 买五量（手）
 *   [19-23] 卖一价 ~ 卖五价
 *   [24-28] 卖一量 ~ 卖五量（手）
 *   [29] 最近逐笔时间（HHmmss）
 *   [30] 最近逐笔价格
 *   [31] 涨跌额
 *   [32] 涨跌幅（%）
 *   [33] 最高价
 *   [34] 最低价
 */

import axios from 'axios'

/** 单只股票实时行情 */
export interface StockTick {
  code: string          // 股票代码（如 600519）
  name: string          // 股票名称（如 茅台）
  price: number         // 当前价格
  prevClose: number     // 昨收价
  open: number          // 今开价
  volume: number        // 成交量（手）
  change: number        // 涨跌额
  changePercent: number // 涨跌幅（%）
  high: number         // 最高价
  low: number          // 最低价
  time: string         // 时间戳 HHmmss
}

/**
 * 解析腾讯接口返回的单条数据
 * raw 格式（已去掉 v_sh600519=" 和尾部的 ";）：
 *   1~茅台~600519~1384.79~1401.17~...
 */
function parseLine(code: string, raw: string): StockTick | null {
  if (!raw || raw.startsWith('v_')) return null

  const parts = raw.split('~')
  if (parts.length < 35) return null

  const price = parseFloat(parts[3]) || 0
  const prevClose = parseFloat(parts[4]) || 0

  return {
    code,
    name: parts[1] || code,
    price,
    prevClose,
    open: parseFloat(parts[5]) || 0,
    volume: parseInt(parts[6]) || 0,
    change: parseFloat(parts[31]) || 0,
    changePercent: parseFloat(parts[32]) || 0,
    high: parseFloat(parts[33]) || 0,
    low: parseFloat(parts[34]) || 0,
    time: parts[29] || '',
  }
}

/**
 * 根据代码生成腾讯接口查询前缀
 * 沪市：sh600519，深市：sz000001
 */
function toTencPrefix(code: string): string {
  if (/^(60|68)/.test(code)) return `sh${code}`
  if (/^(00|30|02)/.test(code)) return `sz${code}`
  return `sh${code}`
}

/**
 * 批量获取实时行情（前端直接调用，无需后端）
 * 使用 axios + arraybuffer 正确处理 GBK 编码
 * @param codes - 股票代码数组，如 ['600519', '000001']
 */
export async function fetchMarketTicks(codes: string[]): Promise<Record<string, StockTick>> {
  if (codes.length === 0) return {}

  const query = codes.map(toTencPrefix).join(',')
  const url = `https://qt.gtimg.cn/q=${query}`

  const result: Record<string, StockTick> = {}

  try {
    const res = await axios.get(url, { responseType: 'arraybuffer' })
    const decoder = new TextDecoder('gbk')
    const text = decoder.decode(new Uint8Array(res.data as ArrayBuffer))


    // 逐行解析：v_sh600519="1~茅台~600519~...";
    const lines = text.split(';\n').filter(Boolean)
    for (const line of lines) {
      // 匹配 v_sh600036="..." 或 v_sh600036='...'
      const match = line.match(/^v_([a-z]+\d+)=\s*["']([^"']+)["']\s*$/)
      if (!match) continue

      const fullCode = match[1] // sh600036 或 sz000001
      const rawData = match[2]
      const code = fullCode.replace(/^[a-z]+/, '') // 去掉前缀，得到 600036
      const tick = parseLine(code, rawData)
      if (tick) {
        result[code] = tick
      }
    }
  } catch (e) {
    console.error('【获取实时行情失败】', e)
  }

  return result
}
