# Node.js Express With dd-trace: Complete Example

## Project Structure

```text
nodejs-express/
  package.json
  tracer.js          (initialize dd-trace FIRST)
  server.js          (import tracer before everything)
  routes/orders.js
  logger.js          (Winston with trace injection)
```

## package.json

```json
{
  "name": "orders-api-node",
  "version": "1.0.0",
  "main": "server.js",
  "dependencies": {
    "dd-trace": "^5.0.0",
    "express": "^4.18.0",
    "winston": "^3.11.0"
  }
}
```

## tracer.js

```javascript
'use strict'

const tracer = require('dd-trace').init({
  service: process.env.DD_SERVICE || 'orders-api-node',
  env: process.env.DD_ENV || 'dev',
  version: process.env.DD_VERSION || '1.0.0',
  hostname: process.env.DD_AGENT_HOST || 'localhost',
  port: parseInt(process.env.DD_TRACE_AGENT_PORT || '8126'),
  logInjection: true,
  runtimeMetrics: true,
  sampleRate: 1.0,
})

module.exports = tracer
```

## logger.js

```javascript
'use strict'

require('./tracer')  // tracer must be initialized before logger

const winston = require('winston')

const logger = winston.createLogger({
  level: 'info',
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.json()   // JSON required for dd-trace log injection
  ),
  defaultMeta: {
    service: process.env.DD_SERVICE || 'orders-api-node',
    env: process.env.DD_ENV || 'dev',
    version: process.env.DD_VERSION || '1.0.0',
  },
  transports: [
    new winston.transports.Console()
  ],
})

module.exports = logger
```

## server.js

```javascript
'use strict'

// CRITICAL: tracer must be first.
require('./tracer')

const express = require('express')
const logger = require('./logger')
const ordersRouter = require('./routes/orders')

const app = express()
app.use(express.json())
app.use('/orders', ordersRouter)

app.listen(3000, () => {
  logger.info('Orders API server started on port 3000')
})
```

## routes/orders.js

```javascript
'use strict'

const express = require('express')
const tracer = require('dd-trace')
const logger = require('../logger')

const router = express.Router()

router.post('/', async (req, res) => {
  const span = tracer.scope().active()

  // Add business context to the auto-created span.
  if (span) {
    span.setTag('order.customer_id', req.body.customerId)
    span.setTag('order.total', req.body.total)
    span.setTag('resource.name', 'POST /orders')
  }

  logger.info('Creating order', {
    customerId: req.body.customerId,
    total: req.body.total,
  })

  // Manual child span for validation.
  const validationSpan = tracer.startSpan('order.validate', {
    childOf: span,
    tags: {
      'order.customer_id': req.body.customerId,
    }
  })

  try {
    await validateOrder(req.body)
    validationSpan.setTag('validation.result', 'passed')
  } catch (err) {
    validationSpan.setTag('error', true)
    validationSpan.setTag('error.message', err.message)
    throw err
  } finally {
    validationSpan.finish()
  }

  const orderId = `ORD-${Date.now()}`
  logger.info('Order created', { orderId })
  res.status(201).json({ orderId, status: 'CREATED' })
})

router.get('/:id', (req, res) => {
  logger.info('Fetching order', { orderId: req.params.id })
  res.json({ orderId: req.params.id, status: 'PENDING' })
})

async function validateOrder(order) {
  if (!order.customerId) throw new Error('customerId is required')
  if (!order.total || order.total <= 0) throw new Error('total must be positive')
}

module.exports = router
```

## docker-compose.yml

```yaml
version: "3.8"

services:
  datadog-agent:
    image: gcr.io/datadoghq/agent:7
    environment:
      DD_API_KEY: "${DD_API_KEY}"
      DD_SITE: datadoghq.com
      DD_APM_ENABLED: "true"
      DD_APM_NON_LOCAL_TRAFFIC: "true"
      DD_DOGSTATSD_NON_LOCAL_TRAFFIC: "true"
    ports:
      - "8125:8125/udp"
      - "8126:8126/tcp"

  orders-api-node:
    build: .
    environment:
      DD_SERVICE: orders-api-node
      DD_ENV: dev
      DD_VERSION: "1.0.0"
      DD_AGENT_HOST: datadog-agent
      DD_TRACE_AGENT_PORT: "8126"
      DD_LOGS_INJECTION: "true"
      DD_TRACE_SAMPLE_RATE: "1.0"
    ports:
      - "3000:3000"
    depends_on:
      - datadog-agent
```

## Expected Log Output (Winston with dd-trace injection)

```json
{
  "level": "info",
  "message": "Creating order",
  "service": "orders-api-node",
  "env": "dev",
  "version": "1.0.0",
  "customerId": "CUST-001",
  "total": 99.99,
  "dd.trace_id": "7712345678901234567",
  "dd.span_id": "8812345678901234567",
  "dd.service": "orders-api-node",
  "dd.env": "dev",
  "dd.version": "1.0.0",
  "timestamp": "2024-01-15T10:23:45.123Z"
}
```
