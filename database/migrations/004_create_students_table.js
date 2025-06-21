/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
exports.up = function(knex) {
  return knex.schema.createTable('students', function(table) {
    table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    table.uuid('classroom_id').notNullable().references('id').inTable('classrooms').onDelete('CASCADE');
    
    // Anonymous demographic data (COPPA compliant - no PII)
    table.string('anonymous_id', 50).notNullable().unique(); // Generated unique identifier
    table.integer('grade_level').nullable();
    table.string('gender', 20).nullable(); // Optional demographic for analytics
    table.string('ethnicity', 50).nullable(); // Optional demographic for analytics
    table.boolean('has_disabilities').nullable(); // For accessibility insights
    table.string('primary_language', 50).nullable();
    
    // Session and engagement data
    table.timestamp('first_joined_at').defaultTo(knex.fn.now());
    table.timestamp('last_active_at').defaultTo(knex.fn.now());
    table.integer('total_sessions').defaultTo(0);
    table.boolean('is_active').defaultTo(true);
    
    table.timestamps(true, true);
    
    // Indexes
    table.index(['classroom_id']);
    table.index(['anonymous_id']);
    table.index(['grade_level']);
    table.index(['is_active']);
    table.index(['last_active_at']);
  });
};

/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
exports.down = function(knex) {
  return knex.schema.dropTable('students');
};