/**
 * IP 粗定位结果。
 */
export interface IpLocationResult {
  /** 粗定位纬度。 */
  latitude: number
  /** 粗定位经度。 */
  longitude: number
  /** 粗定位位置名称，通常只能到城市或运营商出口所在地。 */
  locationName?: string
  /** 粗定位服务名称，便于后续排查来源。 */
  provider: string
}

/**
 * IP 定位接口原始响应。
 */
interface IpLocationPayload {
  /** 字段 success：ipwho.is 返回的成功标识。 */
  success?: boolean
  /** 字段 error：ipapi.co 返回的错误标识。 */
  error?: boolean
  /** 字段 latitude：粗定位纬度。 */
  latitude?: number | string
  /** 字段 longitude：粗定位经度。 */
  longitude?: number | string
  /** 字段 city：城市名称。 */
  city?: string
  /** 字段 region：省份或地区名称。 */
  region?: string
  /** 字段 country：国家名称。 */
  country?: string
  /** 字段 country_name：ipapi.co 返回的国家名称。 */
  country_name?: string
  /** 字段 reason：ipapi.co 返回的错误原因。 */
  reason?: string
  /** 字段 message：ipwho.is 返回的错误原因。 */
  message?: string
}

/** IP 粗定位请求超时时间，避免公共服务异常拖慢页面。 */
const IP_LOCATION_TIMEOUT_MS = 5000

/** 无 token 的公网 IP 粗定位服务，按优先级依次尝试。 */
const IP_LOCATION_PROVIDERS = [
  { name: 'ipapi.co', url: 'https://ipapi.co/json/' },
  { name: 'ipwho.is', url: 'https://ipwho.is/' }
]

/**
 * 通过公网 IP 粗略定位当前访问终端。
 *
 * 实现步骤：
 * 1. 依次调用无 token 的公网 IP 定位服务；
 * 2. 任一服务返回合法经纬度即停止；
 * 3. 全部失败时抛出异常，由调用方回退配置地区。
 */
export async function locateByPublicIp(): Promise<IpLocationResult> {
  const errors: string[] = []
  for (const provider of IP_LOCATION_PROVIDERS) {
    try {
      const payload = await fetchIpLocation(provider.url)
      return normalizeIpLocation(provider.name, payload)
    } catch (error) {
      errors.push(error instanceof Error ? error.message : `${provider.name} 定位失败`)
    }
  }
  throw new Error(errors[0] || '公网 IP 粗定位不可用')
}

/**
 * 调用单个 IP 粗定位接口。
 */
async function fetchIpLocation(url: string): Promise<IpLocationPayload> {
  const controller = new AbortController()
  const timer = window.setTimeout(() => controller.abort(), IP_LOCATION_TIMEOUT_MS)
  try {
    const response = await fetch(url, {
      method: 'GET',
      mode: 'cors',
      cache: 'no-store',
      credentials: 'omit',
      signal: controller.signal
    })
    if (!response.ok) {
      throw new Error(`IP 粗定位响应异常：${response.status}`)
    }
    return (await response.json()) as IpLocationPayload
  } finally {
    window.clearTimeout(timer)
  }
}

/**
 * 规范化不同服务的 IP 粗定位响应。
 */
function normalizeIpLocation(provider: string, payload: IpLocationPayload): IpLocationResult {
  if (payload.error || payload.success === false) {
    throw new Error(payload.reason || payload.message || `${provider} 定位失败`)
  }
  const latitude = parseCoordinate(payload.latitude)
  const longitude = parseCoordinate(payload.longitude)
  if (!validCoordinate(latitude, longitude)) {
    throw new Error(`${provider} 未返回合法经纬度`)
  }
  return {
    latitude,
    longitude,
    locationName: joinLocationName(payload.country_name || payload.country, payload.region, payload.city),
    provider
  }
}

/**
 * 解析经纬度数值。
 */
function parseCoordinate(value: number | string | undefined) {
  const coordinate = Number(value)
  return Number.isFinite(coordinate) ? Number(coordinate.toFixed(6)) : Number.NaN
}

/**
 * 判断经纬度是否在合法范围内。
 */
function validCoordinate(latitude: number, longitude: number) {
  return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180
}

/**
 * 拼接去重后的位置名称。
 */
function joinLocationName(...parts: Array<string | undefined>) {
  const names = parts.map((part) => (part || '').trim()).filter(Boolean)
  return Array.from(new Set(names)).join('')
}
