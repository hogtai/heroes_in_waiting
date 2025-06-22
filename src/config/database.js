const knex = require('knex');
const logger = require('../utils/logger');

const dbConfig = {
  client: 'postgresql',
  connection: {
    host: process.env.DB_HOST || 'localhost',
    port: process.env.DB_PORT || 5432,
    user: process.env.DB_USER || 'postgres',
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME || 'heroes_in_waiting',
    ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false
  },
  pool: {
    min: parseInt(process.env.DB_POOL_MIN) || 2,
    max: parseInt(process.env.DB_POOL_MAX) || 10,
    acquireTimeoutMillis: 30000,
    createTimeoutMillis: 30000,
    destroyTimeoutMillis: 5000,
    idleTimeoutMillis: 30000,
    reapIntervalMillis: 1000,
    createRetryIntervalMillis: 200
  },
  migrations: {
    directory: '../database/migrations'
  },
  seeds: {
    directory: '../database/seeds'
  },
  debug: process.env.NODE_ENV === 'development'
};

const db = knex(dbConfig);

// Connection pool monitoring
db.on('query', (query) => {
  if (process.env.NODE_ENV === 'development') {
    logger.debug('Database query:', {
      sql: query.sql,
      bindings: query.bindings,
      duration: query.duration
    });
  }
});

db.on('query-error', (error, query) => {
  logger.error('Database query error:', {
    error: error.message,
    sql: query.sql,
    bindings: query.bindings
  });
});

// Test database connection
db.raw('SELECT 1')
  .then(() => {
    logger.info(`Database connected successfully in ${process.env.NODE_ENV || 'development'} mode`);
  })
  .catch((error) => {
    logger.error('Database connection failed:', error);
  });

module.exports = db;