/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
exports.up = function(knex) {
  return knex.schema.createTable('classroom_sessions', function(table) {
    table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    table.uuid('classroom_id').notNullable().references('id').inTable('classrooms').onDelete('CASCADE');
    table.uuid('lesson_id').notNullable().references('id').inTable('lessons').onDelete('CASCADE');
    table.uuid('facilitator_id').notNullable().references('id').inTable('facilitators').onDelete('CASCADE');
    
    table.timestamp('session_date').notNullable();
    table.timestamp('started_at').nullable();
    table.timestamp('ended_at').nullable();
    table.integer('duration_minutes').nullable();
    table.integer('students_present').defaultTo(0);
    table.string('session_status', 50).defaultTo('scheduled'); // scheduled, active, completed, cancelled
    table.text('facilitator_notes').nullable();
    table.json('session_data').nullable(); // Additional session metadata
    
    table.timestamps(true, true);
    
    // Indexes
    table.index(['classroom_id']);
    table.index(['lesson_id']);
    table.index(['facilitator_id']);
    table.index(['session_date']);
    table.index(['session_status']);
  });
};

/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
exports.down = function(knex) {
  return knex.schema.dropTable('classroom_sessions');
};