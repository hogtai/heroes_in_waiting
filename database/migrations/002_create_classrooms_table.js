/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
exports.up = function(knex) {
  return knex.schema.createTable('classrooms', function(table) {
    table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    table.uuid('facilitator_id').notNullable().references('id').inTable('facilitators').onDelete('CASCADE');
    table.string('name', 255).notNullable();
    table.text('description').nullable();
    table.string('classroom_code', 10).unique().notNullable();
    table.integer('grade_level').nullable();
    table.integer('student_capacity').defaultTo(30);
    table.boolean('is_active').defaultTo(true);
    table.timestamp('code_expires_at').nullable();
    table.timestamps(true, true);
    
    // Indexes
    table.index(['facilitator_id']);
    table.index(['classroom_code']);
    table.index(['is_active']);
    table.index(['created_at']);
  });
};

/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
exports.down = function(knex) {
  return knex.schema.dropTable('classrooms');
};