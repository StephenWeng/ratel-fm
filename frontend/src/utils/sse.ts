/**
 * 简单 SSE 文本解析器，用于 fetch POST + ReadableStream 场景。
 */
export interface SseMessage {
  /**
   * 字段 event：表示 SSE 事件名。
   */
  event: string
  /**
   * 字段 data：表示 SSE data 文本。
   */
  data: string
}

/**
 * 创建增量 SSE 解析器。
 *
 * 实现步骤：
 * 1. 累积上一次未读完的文本块；
 * 2. 按空行拆分 SSE 事件；
 * 3. 合并多行 data 并返回事件名和数据。
 */
export function createSseParser(onMessage: (message: SseMessage) => void) {
  let buffer = ''
  return {
    /**
     * 推入一段响应文本。
     */
    feed(chunk: string) {
      buffer += chunk
      const parts = buffer.split(/\r?\n\r?\n/)
      buffer = parts.pop() || ''
      for (const part of parts) {
        const message = parseSseMessage(part)
        if (message) {
          onMessage(message)
        }
      }
    },
    /**
     * 结束解析，处理最后一段没有空行结尾的数据。
     */
    end() {
      if (!buffer.trim()) {
        return
      }
      const message = parseSseMessage(buffer)
      buffer = ''
      if (message) {
        onMessage(message)
      }
    }
  }
}

/**
 * 解析单条 SSE 事件。
 */
function parseSseMessage(text: string): SseMessage | undefined {
  let event = 'message'
  const data: string[] = []
  for (const line of text.split(/\r?\n/)) {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      data.push(line.slice(5).trimStart())
    }
  }
  if (!data.length) {
    return undefined
  }
  return {
    event,
    data: data.join('\n')
  }
}
