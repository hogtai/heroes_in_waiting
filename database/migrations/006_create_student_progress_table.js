/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
exports.up = function(knex) {
  return knex.schema.createTable('student_progress', function(table) {
    table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    table.uuid('student_id').notNullable().references('id').inTable('students').onDelete('CASCADE');
    table.uuid('lesson_id').notNullable().references('id').inTable('lessons').onDelete('CASCADE');
    table.uuid('classroom_session_id').nullable().references('id').inTable('classroom_sessions').onDelete('SET NULL');
    
    // Progress tracking
    table.string('completion_status', 50).defaultTo('not_started'); // not_started, in_progress, completed
    table.integer('progress_percentage').defaultTo(0);
    table.timestamp('started_at').nullable();
    table.timestamp('completed_at').nullable();
    table.integer('time_spent_minutes').defaultTo(0);
    
    // Engagement metrics
    table.integer('video_watch_percentage').defaultTo(0);
    table.integer('activities_completed').defaultTo(0);
    table.integer('total_activities').defaultTo(0);
    table.json('activity_responses').nullable(); // Anonymous responses to activities
    
    table.timestamps(true, true);
    
    // Indexes
    table.index(['student_id']);
    table.index(['lesson_id']);
    table.index(['classroom_session_id']);
    table.index(['completion_status']);
    table.index(['completed_at']);
    
    // Unique constraint to prevent duplicate progress records
    table.unique(['student_id', 'lesson_id']);
  });
};

/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
exports.down = function(knex) {
  return knex.schema.dropTable('student_progress');
};