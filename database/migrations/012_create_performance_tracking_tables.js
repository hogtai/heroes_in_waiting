/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
exports.up = function(knex) {
  return knex.schema
    // Performance metrics table for tracking API and content performance
    .createTable('performance_metrics', function(table) {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.string('content_type', 50).notNullable(); // api, lesson_content, media, sync
      table.integer('load_time_ms').notNullable();
      table.string('device_type', 20).notNullable(); // mobile, tablet, desktop
      table.string('network_type', 20).nullable(); // wifi, cellular, unknown
      table.string('user_type', 20).notNullable(); // facilitator, student, anonymous
      table.json('metadata').nullable(); // Additional performance context
      table.timestamp('timestamp').defaultTo(knex.fn.now());
      table.timestamps(true, true);
      
      // Indexes for performance analysis
      table.index(['content_type', 'timestamp']);
      table.index(['device_type', 'timestamp']);
      table.index(['user_type', 'timestamp']);
      table.index(['load_time_ms']);
    })
    
    // Performance alerts for slow operations
    .createTable('performance_alerts', function(table) {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.string('alert_type', 50).notNullable(); // slow_response, error, timeout
      table.string('endpoint', 255).notNullable();
      table.integer('response_time').notNullable();
      table.integer('status_code').nullable();
      table.string('user_type', 20).nullable();
      table.text('user_agent').nullable();
      table.json('additional_data').nullable();
      table.boolean('resolved').defaultTo(false);
      table.timestamp('created_at').defaultTo(knex.fn.now());
      table.timestamp('resolved_at').nullable();
      
      // Indexes for alert management
      table.index(['alert_type', 'created_at']);
      table.index(['resolved', 'created_at']);
      table.index(['endpoint']);
    })
    
    // Cache performance tracking
    .createTable('cache_performance', function(table) {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.string('cache_key', 255).notNullable();
      table.boolean('cache_hit').notNullable();
      table.integer('load_time_ms').notNullable();
      table.string('cache_type', 50).defaultTo('memory'); // memory, redis, file
      table.json('cache_metadata').nullable();
      table.timestamp('timestamp').defaultTo(knex.fn.now());
      
      // Indexes for cache analysis
      table.index(['cache_key', 'timestamp']);
      table.index(['cache_hit', 'timestamp']);
      table.index(['cache_type']);
    })
    
    // Mobile-specific performance metrics
    .createTable('mobile_performance', function(table) {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.string('session_id', 100).nullable(); // For grouping related requests
      table.string('action_type', 50).notNullable(); // sync, download, view, navigate
      table.integer('duration_ms').notNullable();
      table.string('device_model', 100).nullable();
      table.string('os_version', 50).nullable();
      table.string('app_version', 20).nullable();
      table.string('network_quality', 20).nullable(); // excellent, good, fair, poor
      table.integer('memory_usage_mb').nullable();
      table.integer('battery_level').nullable();
      table.json('performance_data').nullable(); // Detailed metrics
      table.string('user_type', 20).notNullable();
      table.timestamp('timestamp').defaultTo(knex.fn.now());
      
      // Indexes for mobile analysis
      table.index(['action_type', 'timestamp']);
      table.index(['device_model']);
      table.index(['network_quality', 'timestamp']);
      table.index(['user_type', 'timestamp']);
    })
    
    // Educational content performance tracking
    .createTable('educational_performance', function(table) {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.uuid('lesson_id').nullable().references('id').inTable('lessons');
      table.uuid('content_version_id').nullable().references('id').inTable('content_versions');
      table.string('interaction_type', 50).notNullable(); // view, download, complete, navigate
      table.integer('response_time_ms').notNullable();
      table.string('grade_level', 20).defaultTo('grades-4-6');
      table.string('device_context', 50).nullable(); // classroom, home, mobile
      table.boolean('offline_mode').defaultTo(false);
      table.json('educational_metrics').nullable(); // Engagement, completion rates
      table.string('user_type', 20).notNullable();
      table.timestamp('timestamp').defaultTo(knex.fn.now());
      
      // Indexes for educational analysis
      table.index(['lesson_id', 'timestamp']);
      table.index(['interaction_type', 'timestamp']);
      table.index(['device_context']);
      table.index(['offline_mode']);
    });
};

/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
exports.down = function(knex) {
  return knex.schema
    .dropTableIfExists('educational_performance')
    .dropTableIfExists('mobile_performance')
    .dropTableIfExists('cache_performance')
    .dropTableIfExists('performance_alerts')
    .dropTableIfExists('performance_metrics');
};