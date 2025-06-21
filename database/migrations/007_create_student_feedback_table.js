/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
exports.up = function(knex) {
  return knex.schema.createTable('student_feedback', function(table) {
    table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    table.uuid('student_id').notNullable().references('id').inTable('students').onDelete('CASCADE');
    table.uuid('lesson_id').notNullable().references('id').inTable('lessons').onDelete('CASCADE');
    table.uuid('classroom_session_id').nullable().references('id').inTable('classroom_sessions').onDelete('SET NULL');
    
    // Feedback types
    table.string('feedback_type', 50).notNullable(); // mood_checkin, reflection, activity_response, lesson_rating
    table.string('feedback_category', 50).nullable(); // bullying_awareness, empathy, confidence, etc.
    
    // Feedback content (anonymous)
    table.integer('rating_value').nullable(); // 1-5 scale ratings
    table.text('text_response').nullable(); // Anonymous text responses
    table.json('structured_response').nullable(); // JSON for structured feedback
    table.string('mood_indicator', 50).nullable(); // happy, sad, confused, excited, etc.
    
    // Context
    table.string('activity_context', 100).nullable(); // Which activity this feedback relates to
    table.timestamp('feedback_date').defaultTo(knex.fn.now());
    
    table.timestamps(true, true);
    
    // Indexes
    table.index(['student_id']);
    table.index(['lesson_id']);
    table.index(['classroom_session_id']);
    table.index(['feedback_type']);
    table.index(['feedback_category']);
    table.index(['feedback_date']);
  });
};

/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
exports.down = function(knex) {
  return knex.schema.dropTable('student_feedback');
};