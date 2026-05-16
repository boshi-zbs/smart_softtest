import request from '@/utils/request'

export function getMyTodoList(params) {
  return request({
    url: '/mytodo',
    method: 'get',
    params
  })
}