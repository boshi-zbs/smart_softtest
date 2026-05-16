import request from '@/utils/request'

// Git仓库配置
export function getGitConfig(projectId) {
  return request({
    url: `/git-config/project/${projectId}`,
    method: 'get'
  })
}

export function saveGitConfig(data) {
  return request({
    url: '/git-config',
    method: 'post',
    data
  })
}

export function syncRepository(projectId) {
  return request({
    url: `/git-config/sync/${projectId}`,
    method: 'post'
  })
}

export function getFileTree(projectId) {
  return request({
    url: `/ai-analysis/code-tree/${projectId}`,
    method: 'get'
  })
}

export function getCodeContent(projectId, filePath) {
  return request({
    url: `/ai-analysis/code-content/${projectId}`,
    method: 'get',
    params: { filePath }
  })
}

export function searchCodeFiles(projectId, keyword) {
  return request({
    url: `/ai-analysis/search-code/${projectId}`,
    method: 'get',
    params: { keyword }
  })
}

// AI分析
export function analyzeSingleDefect(data) {
  return request({
    url: '/ai-analysis/analyze-single',
    method: 'post',
    data
  })
}

export function analyzeBatchDefects(data) {
  return request({
    url: '/ai-analysis/analyze-batch',
    method: 'post',
    data
  })
}

export function getAnalysisHistory() {
  return request({
    url: '/ai-analysis/history',
    method: 'get'
  })
}

// 流式分析（使用EventSource）
export function streamAnalyzeDefect(defectId, filePaths) {
  const url = new URL('/api/ai-analysis/analyze-stream', window.location.origin)
  url.searchParams.append('defectId', defectId)
  if (filePaths && filePaths.length) {
    url.searchParams.append('filePaths', JSON.stringify(filePaths))
  }
  return url.toString()
}

// AI对话
export function getChatHistory(sessionId) {
  return request({
    url: `/ai-analysis/chat-history/${sessionId}`,
    method: 'get'
  })
}

// 流式对话
export function getChatStreamUrl(sessionId, defectId, message) {
  const url = new URL('/api/ai-analysis/chat-stream', window.location.origin)
  url.searchParams.append('sessionId', sessionId)
  if (defectId) {
    url.searchParams.append('defectId', defectId)
  }
  url.searchParams.append('message', message)
  return url.toString()
}

// 流式对话（使用 fetch，支持携带 token）
export async function streamChat(sessionId, defectId, message, filePaths, onChunk, onError, onComplete) {
  const token = localStorage.getItem('token')
  const response = await fetch('/api/ai-analysis/chat-stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({ sessionId, defectId, message, filePaths })
  })

  if (!response.ok) {
    const errorText = await response.text()
    onError?.(new Error(`HTTP ${response.status}: ${errorText}`))
    return
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  try {
    // eslint-disable-next-line no-constant-condition
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      for (const line of lines) {
        if (line.trim() === '') continue
        if (line.startsWith('data: ')) {
          const data = line.slice(6)
          if (data === '[DONE]') {
            onComplete?.()
            return
          }
          onChunk?.(data)
        }
      }
    }
  } catch (err) {
    onError?.(err)
  }
}