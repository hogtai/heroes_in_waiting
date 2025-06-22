# SRE Remediation Plan
**Date:** December 2024  
**Priority:** High - Must complete before Checkpoint 5  
**Status:** In Progress

## Overview
This plan addresses all infrastructure, monitoring, reliability, and operational issues identified in the SRE review before proceeding to Checkpoint 5.

## Critical Infrastructure Issues to Address

### 1. Comprehensive Monitoring Implementation
**Priority:** Critical  
**Status:** In Progress

#### 1.1 Prometheus Configuration
**File:** `monitoring/prometheus.yml`
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "rules/*.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  - job_name: 'heroes-api'
    static_configs:
      - targets: ['api:3000']
    metrics_path: '/metrics'
    scrape_interval: 10s

  - job_name: 'postgres'
    static_configs:
      - targets: ['db:5432']
    scrape_interval: 30s

  - job_name: 'pgbouncer'
    static_configs:
      - targets: ['pgbouncer:5432']
    scrape_interval: 30s

  - job_name: 'redis'
    static_configs:
      - targets: ['redis:6379']
    scrape_interval: 30s

  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']
    scrape_interval: 15s
```

#### 1.2 Grafana Dashboard Configuration
**File:** `monitoring/grafana/dashboards/heroes-dashboard.json`
```json
{
  "dashboard": {
    "title": "Heroes in Waiting - System Overview",
    "panels": [
      {
        "title": "API Response Time",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_request_duration_seconds_sum[5m]) / rate(http_request_duration_seconds_count[5m])",
            "legendFormat": "{{method}} {{route}}"
          }
        ]
      },
      {
        "title": "Database Connections",
        "type": "graph",
        "targets": [
          {
            "expr": "pg_stat_database_numbackends",
            "legendFormat": "{{datname}}"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_requests_total{status=~\"5..\"}[5m])",
            "legendFormat": "5xx Errors"
          }
        ]
      }
    ]
  }
}
```

### 2. Enhanced Logging and Alerting
**Priority:** High  
**Status:** Pending

#### 2.1 Structured Logging Enhancement
**File:** `src/utils/logger.js`
```javascript
const winston = require('winston');
const { format } = winston;

const logFormat = format.combine(
  format.timestamp(),
  format.errors({ stack: true }),
  format.json(),
  format.metadata({ fillExcept: ['message', 'level', 'timestamp'] })
);

const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: logFormat,
  defaultMeta: {
    service: 'heroes-api',
    environment: process.env.NODE_ENV || 'development'
  },
  transports: [
    new winston.transports.Console({
      format: format.combine(
        format.colorize(),
        format.simple()
      )
    }),
    new winston.transports.File({
      filename: 'logs/error.log',
      level: 'error',
      maxsize: 5242880, // 5MB
      maxFiles: 5
    }),
    new winston.transports.File({
      filename: 'logs/combined.log',
      maxsize: 5242880, // 5MB
      maxFiles: 5
    })
  ]
});

// Add request logging middleware
const requestLogger = (req, res, next) => {
  const start = Date.now();
  
  res.on('finish', () => {
    const duration = Date.now() - start;
    logger.info('HTTP Request', {
      method: req.method,
      url: req.url,
      status: res.statusCode,
      duration,
      userAgent: req.get('User-Agent'),
      ip: req.ip,
      userId: req.user?.id || req.facilitator?.id || 'anonymous'
    });
  });
  
  next();
};

module.exports = { logger, requestLogger };
```

#### 2.2 Alerting Rules
**File:** `monitoring/rules/alerts.yml`
```yaml
groups:
  - name: heroes_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} errors per second"

      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High response time detected"
          description: "95th percentile response time is {{ $value }} seconds"

      - alert: DatabaseConnectionHigh
        expr: pg_stat_database_numbackends > 80
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High database connections"
          description: "Database has {{ $value }} active connections"

      - alert: APIDown
        expr: up{job="heroes-api"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "API is down"
          description: "Heroes API is not responding"
```

### 3. Health Checks and Auto-scaling
**Priority:** High  
**Status:** Pending

#### 3.1 Enhanced Health Check
**File:** `healthcheck.js`
```javascript
const db = require('./src/config/database');
const logger = require('./src/utils/logger');

async function healthCheck() {
  const checks = {
    database: false,
    memory: false,
    disk: false
  };

  try {
    // Database health check
    await db.raw('SELECT 1');
    checks.database = true;
  } catch (error) {
    logger.error('Database health check failed:', error);
  }

  // Memory health check
  const memUsage = process.memoryUsage();
  const memUsagePercent = (memUsage.heapUsed / memUsage.heapTotal) * 100;
  checks.memory = memUsagePercent < 90;

  // Disk health check (if applicable)
  checks.disk = true; // Simplified for now

  const isHealthy = Object.values(checks).every(check => check);

  if (!isHealthy) {
    logger.error('Health check failed:', checks);
    process.exit(1);
  }

  logger.info('Health check passed:', checks);
  process.exit(0);
}

healthCheck();
```

#### 3.2 Docker Compose with Monitoring
**File:** `docker-compose.monitoring.yml`
```yaml
version: '3.8'

services:
  # ... existing services ...

  # Prometheus
  prometheus:
    image: prom/prometheus:latest
    container_name: heroes_prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=200h'
      - '--web.enable-lifecycle'
    networks:
      - heroes_network

  # Grafana
  grafana:
    image: grafana/grafana:latest
    container_name: heroes_grafana
    ports:
      - "3001:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./monitoring/grafana/datasources:/etc/grafana/provisioning/datasources
    networks:
      - heroes_network

  # Node Exporter
  node-exporter:
    image: prom/node-exporter:latest
    container_name: heroes_node_exporter
    ports:
      - "9100:9100"
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    command:
      - '--path.procfs=/host/proc'
      - '--path.sysfs=/host/sys'
      - '--collector.filesystem.mount-points-exclude=^/(sys|proc|dev|host|etc)($$|/)'
    networks:
      - heroes_network

  # Alertmanager
  alertmanager:
    image: prom/alertmanager:latest
    container_name: heroes_alertmanager
    ports:
      - "9093:9093"
    volumes:
      - ./monitoring/alertmanager.yml:/etc/alertmanager/alertmanager.yml
      - alertmanager_data:/alertmanager
    command:
      - '--config.file=/etc/alertmanager/alertmanager.yml'
      - '--storage.path=/alertmanager'
    networks:
      - heroes_network

volumes:
  prometheus_data:
  grafana_data:
  alertmanager_data:
```

### 4. Performance Monitoring
**Priority:** Medium  
**Status:** Pending

#### 4.1 API Metrics Middleware
**File:** `src/middleware/metrics.js`
```javascript
const prometheus = require('prom-client');

// Create metrics
const httpRequestDuration = new prometheus.Histogram({
  name: 'http_request_duration_seconds',
  help: 'Duration of HTTP requests in seconds',
  labelNames: ['method', 'route', 'status_code'],
  buckets: [0.1, 0.5, 1, 2, 5]
});

const httpRequestsTotal = new prometheus.Counter({
  name: 'http_requests_total',
  help: 'Total number of HTTP requests',
  labelNames: ['method', 'route', 'status']
});

const activeConnections = new prometheus.Gauge({
  name: 'active_connections',
  help: 'Number of active connections'
});

// Metrics middleware
const metricsMiddleware = (req, res, next) => {
  const start = Date.now();
  
  res.on('finish', () => {
    const duration = Date.now() - start;
    const route = req.route?.path || req.path;
    
    httpRequestDuration
      .labels(req.method, route, res.statusCode)
      .observe(duration / 1000);
    
    httpRequestsTotal
      .labels(req.method, route, res.statusCode.toString())
      .inc();
  });
  
  next();
};

// Metrics endpoint
const metricsEndpoint = async (req, res) => {
  try {
    res.set('Content-Type', prometheus.register.contentType);
    res.end(await prometheus.register.metrics());
  } catch (error) {
    res.status(500).end(error);
  }
};

module.exports = {
  metricsMiddleware,
  metricsEndpoint,
  activeConnections
};
```

## Implementation Timeline

### Phase 1: Basic Monitoring (Day 1)
- [ ] Set up Prometheus and Grafana
- [ ] Configure basic metrics collection
- [ ] Create initial dashboards
- [ ] Test monitoring setup

### Phase 2: Enhanced Logging (Day 2)
- [ ] Implement structured logging
- [ ] Add request logging middleware
- [ ] Configure log rotation
- [ ] Test logging functionality

### Phase 3: Alerting (Day 3)
- [ ] Configure alerting rules
- [ ] Set up Alertmanager
- [ ] Test alert notifications
- [ ] Fine-tune alert thresholds

### Phase 4: Performance Monitoring (Day 4)
- [ ] Add API metrics middleware
- [ ] Configure database monitoring
- [ ] Set up auto-scaling rules
- [ ] Performance testing

## Success Criteria
- [ ] Prometheus and Grafana monitoring operational
- [ ] All critical metrics being collected
- [ ] Alerting system functional
- [ ] Logging system enhanced and structured
- [ ] Health checks comprehensive
- [ ] Performance monitoring in place

## Risk Mitigation
- **Rollback Plan:** Keep monitoring configuration in version control
- **Testing Strategy:** Test all monitoring components thoroughly
- **Documentation:** Update operational runbooks
- **Training:** Ensure team can use monitoring tools

## Next Steps
1. Begin Phase 1 implementation
2. Coordinate with development team for metrics
3. Test all monitoring components
4. Complete all phases before Checkpoint 5 