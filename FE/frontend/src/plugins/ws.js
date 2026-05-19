import mqtt from 'mqtt'

let client = null
const topicHandlers = new Map()

export function initMqtt({ brokerUrl = import.meta.env.VITE_MQTT_URL || 'ws://localhost:9001', clientId = 'frontend-' + Math.random().toString(36).slice(2) } = {}) {
  if (client) return client

  client = mqtt.connect(brokerUrl, {
    clientId,
    reconnectPeriod: 5000,
    clean: true
  })

  client.on('connect', () => {
    console.log('MQTT connected')
  })
  client.on('error', (err) => console.error('MQTT error', err))
  client.on('close', () => {
    console.log('MQTT disconnected')
  })
  client.on('message', (receivedTopic, message) => {
    const handlers = topicHandlers.get(receivedTopic)
    if (!handlers || handlers.size === 0) return

    const payload = message.toString()
    let parsedPayload = payload

    try {
      parsedPayload = JSON.parse(payload)
    } catch {
      // Payload is plain text and can be passed as-is.
    }

    handlers.forEach((handler) => {
      try {
        handler(parsedPayload)
      } catch (error) {
        console.error('MQTT message handler error', error)
      }
    })
  })

  return client
}

export function subscribe(topic, cb) {
  if (!client) throw new Error('MQTT client not initialized')

  if (!topicHandlers.has(topic)) {
    topicHandlers.set(topic, new Set())
  }

  topicHandlers.get(topic).add(cb)

  client.subscribe(topic, (err) => {
    if (err) console.error('Subscribe error', err)
  })

  return () => {
    const handlers = topicHandlers.get(topic)
    if (!handlers) return

    handlers.delete(cb)
    if (handlers.size === 0) {
      topicHandlers.delete(topic)
      client.unsubscribe(topic, (err) => {
        if (err) console.error('Unsubscribe error', err)
      })
    }
  }
}

export function publish(topic, message) {
  if (!client) throw new Error('MQTT client not initialized')

  const payload = typeof message === 'string' ? message : JSON.stringify(message)
  client.publish(topic, payload)
}

export default { initMqtt, subscribe, publish }
