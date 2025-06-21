/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
exports.up = function(knex) {
  return knex.schema.createTable('lessons', function(table) {
    table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    table.integer('lesson_number').notNullable();
    table.string('title', 255).notNullable();
    table.text('description').nullable();
    table.text('learning_objectives').nullable();
    table.integer('duration_minutes').nullable();
    table.string('difficulty_level', 50).nullable();
    table.json('content_structure').nullable(); // JSON structure for lesson content
    table.string('video_url', 500).nullable();
    table.string('video_thumbnail', 500).nullable();
    table.integer('video_duration_seconds').nullable();
    table.json('downloadable_resources').nullable(); // Array of resource URLs
    table.json('activities').nullable(); // Array of activity definitions
    table.json('discussion_questions').nullable(); // Array of discussion prompts
    table.boolean('is_published').defaultTo(false);
    table.integer('sort_order').nullable();
    table.timestamps(true, true);
    
    // Indexes
    table.index(['lesson_number']);
    table.index(['is_published']);
    table.index(['sort_order']);
    
    // Constraints
    table.unique(['lesson_number']);
  });
};

/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
exports.down = function(knex) {
  return knex.schema.dropTable('lessons');
};