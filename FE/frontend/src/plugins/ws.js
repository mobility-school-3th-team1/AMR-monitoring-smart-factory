import mqtt from 'mqtt'

let client = null

export function initMqtt({ brokerUrl = 'mqtt://localhost:8080', clientId = 'frontend-' + Math.random().toString(36).slice(2) } = {}) {
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

  return client
}

export function subscribe(topic, cb) {
  if (!client) throw new Error('MQTT client not initialized')
  client.subscribe(topic, (err) => {
    if (err) console.error('Subscribe error', err)
  })
  client.on('message', (receivedTopic, message) => {
    if (receivedTopic === topic) {
      const payload = message.toString()
      try {
        cb(JSON.parse(payload))
      } catch (e) {
        cb(payload)
      }
    }
  })
}

export function publish(topic, message) {
  if (!client) throw new Error('MQTT client not initialized')
  client.publish(topic, JSON.stringify(message))
}

export default { initMqtt, subscribe, publish }
