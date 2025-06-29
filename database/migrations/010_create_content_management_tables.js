/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
exports.up = function(knex) {
  return knex.schema
    // Content versions table for version control
    .createTable('content_versions', function(table) {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.uuid('lesson_id').notNullable().references('id').inTable('lessons').onDelete('CASCADE');
      table.integer('version_number').notNullable();
      table.string('title', 255).notNullable();
      table.text('description').nullable();
      table.json('content_structure').notNullable(); // Full lesson content structure
      table.json('metadata').nullable(); // Additional version metadata
      table.uuid('created_by').notNullable().references('id').inTable('facilitators');
      table.string('change_summary', 500).nullable(); // Brief description of changes
      table.string('status', 50).defaultTo('draft').notNullable(); // draft, review, approved, published
      table.timestamp('reviewed_at').nullable();
      table.uuid('reviewed_by').nullable().references('id').inTable('facilitators');
      table.text('review_notes').nullable();
      table.timestamps(true, true);
      
      // Indexes
      table.index(['lesson_id', 'version_number']);
      table.index(['status']);
      table.index(['created_by']);
      
      // Constraints
      table.unique(['lesson_id', 'version_number']);
    })
    
    // Media files table for content assets
    .createTable('media_files', function(table) {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.string('file_name', 255).notNullable();
      table.string('original_name', 255).notNullable();
      table.string('file_path', 500).notNullable();
      table.string('mime_type', 100).notNullable();
      table.bigInteger('file_size_bytes').notNullable();
      table.string('file_hash', 64).notNullable(); // SHA-256 hash for deduplication
      table.string('media_type', 50).notNullable(); // video, audio, image, document
      table.json('metadata').nullable(); // Duration, dimensions, etc.
      table.uuid('uploaded_by').notNullable().references('id').inTable('facilitators');
      table.boolean('is_public').defaultTo(false);
      table.string('access_level', 50).defaultTo('private'); // private, classroom, public
      table.timestamps(true, true);
      
      // Indexes
      table.index(['media_type']);
      table.index(['uploaded_by']);
      table.index(['file_hash']);
      table.index(['is_public']);
    })
    
    // Content approval workflow table
    .createTable('content_approvals', function(table) {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.uuid('content_version_id').notNullable().references('id').inTable('content_versions').onDelete('CASCADE');
      table.uuid('requested_by').notNullable().references('id').inTable('facilitators');
      table.uuid('assigned_to').nullable().references('id').inTable('facilitators');
      table.string('status', 50).defaultTo('pending').notNullable(); // pending, in_review, approved, rejected
      table.text('request_notes').nullable();
      table.text('review_notes').nullable();
      table.timestamp('requested_at').defaultTo(knex.fn.now());
      table.timestamp('reviewed_at').nullable();
      table.timestamps(true, true);
      
      // Indexes
      table.index(['content_version_id']);
      table.index(['status']);
      table.index(['assigned_to']);
      table.index(['requested_by']);
    })
    
    // Content categories and tags
    .createTable('content_categories', function(table) {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.string('name', 100).notNullable();
      table.text('description').nullable();
      table.string('color', 7).nullable(); // Hex color code
      table.uuid('parent_id').nullable().references('id').inTable('content_categories');
      table.integer('sort_order').defaultTo(0);
      table.boolean('is_active').defaultTo(true);
      table.timestamps(true, true);
      
      // Indexes
      table.index(['parent_id']);
      table.index(['is_active']);
      table.unique(['name']);
    })
    
    // Content tags
    .createTable('content_tags', function(table) {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.string('name', 50).notNullable();
      table.text('description').nullable();
      table.boolean('is_active').defaultTo(true);
      table.timestamps(true, true);
      
      // Indexes
      table.index(['is_active']);
      table.unique(['name']);
    })
    
    // Junction table for content version tags
    .createTable('content_version_tags', function(table) {
      table.uuid('content_version_id').notNullable().references('id').inTable('content_versions').onDelete('CASCADE');
      table.uuid('tag_id').notNullable().references('id').inTable('content_tags').onDelete('CASCADE');
      table.primary(['content_version_id', 'tag_id']);
    })
    
    // Content usage analytics
    .createTable('content_analytics', function(table) {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.uuid('content_version_id').notNullable().references('id').inTable('content_versions').onDelete('CASCADE');
      table.uuid('classroom_id').nullable().references('id').inTable('classrooms').onDelete('CASCADE');
      table.string('event_type', 50).notNullable(); // viewed, downloaded, shared, rated
      table.json('event_data').nullable(); // Additional event-specific data
      table.string('user_type', 20).notNullable(); // facilitator, student
      table.uuid('user_id').nullable(); // Can be null for anonymous events
      table.timestamp('event_timestamp').defaultTo(knex.fn.now());
      table.timestamps(true, true);
      
      // Indexes
      table.index(['content_version_id']);
      table.index(['classroom_id']);
      table.index(['event_type']);
      table.index(['event_timestamp']);
    });
};

/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
exports.down = function(knex) {
  return knex.schema
    .dropTableIfExists('content_analytics')
    .dropTableIfExists('content_version_tags')
    .dropTableIfExists('content_tags')
    .dropTableIfExists('content_categories')
    .dropTableIfExists('content_approvals')
    .dropTableIfExists('media_files')
    .dropTableIfExists('content_versions');
}; 