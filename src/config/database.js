const knex = require('knex');
const knexConfig = require('../../knexfile');

const environment = process.env.NODE_ENV || 'development';
const config = knexConfig[environment];

const db = knex(config);

// Test database connection
db.raw('SELECT 1')
  .then(() => {
    console.log(`Database connected successfully in ${environment} mode`);
  })
  .catch((error) => {
    console.error('Database connection failed:', error);
  });

module.exports = db;