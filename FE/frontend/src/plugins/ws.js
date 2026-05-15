import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

let client = null

export function initWS({ url = '/api/v1/stream', token = null } = {}) {
  if (client) return client

  client = new Client({
    webSocketFactory: () => new SockJS(url),
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    debug: false,
    reconnectDelay: 5000
  })

  client.onConnect = () => {
    console.log('STOMP connected')
  }
  client.onStompError = (err) => console.error('STOMP error', err)
  client.activate()
  return client
}

export function subscribe(topic, cb) {
  if (!client) throw new Error('STOMP client not initialized')
  return client.subscribe(topic, (msg) => {
    const body = msg.body ? JSON.parse(msg.body) : null
    cb(body)
  })
}
