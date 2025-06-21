/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
exports.up = function(knex) {
  return knex.schema.createTable('analytics_events', function(table) {
    table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    table.uuid('student_id').nullable().references('id').inTable('students').onDelete('CASCADE');
    table.uuid('facilitator_id').nullable().references('id').inTable('facilitators').onDelete('CASCADE');
    table.uuid('classroom_id').nullable().references('id').inTable('classrooms').onDelete('CASCADE');
    table.uuid('lesson_id').nullable().references('id').inTable('lessons').onDelete('CASCADE');
    table.uuid('session_id').nullable().references('id').inTable('classroom_sessions').onDelete('CASCADE');
    
    // Event details
    table.string('event_type', 100).notNullable(); // app_open, lesson_start, activity_complete, etc.
    table.string('event_category', 50).notNullable(); // engagement, progress, interaction, etc.
    table.string('event_action', 100).notNullable(); // click, view, complete, submit, etc.
    table.string('event_label', 200).nullable(); // Additional context
    table.integer('event_value').nullable(); // Numeric value if applicable
    
    // Event metadata
    table.json('event_properties').nullable(); // Additional event data
    table.string('device_type', 50).nullable(); // mobile, tablet, chromebook
    table.string('platform', 50).nullable(); // android, ios, web
    table.string('app_version', 20).nullable();
    
    // Session context
    table.string('session_id_local', 100).nullable(); // Local session identifier
    table.integer('session_duration').nullable(); // Duration in seconds
    table.timestamp('event_timestamp').defaultTo(knex.fn.now());
    
    table.timestamps(true, true);
    
    // Indexes
    table.index(['student_id']);
    table.index(['facilitator_id']);
    table.index(['classroom_id']);
    table.index(['lesson_id']);
    table.index(['event_type']);
    table.index(['event_category']);
    table.index(['event_timestamp']);
  });
};

/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
exports.down = function(knex) {
  return knex.schema.dropTable('analytics_events');
};