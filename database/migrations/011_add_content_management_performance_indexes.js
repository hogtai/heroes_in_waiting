/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
exports.up = function(knex) {
  return knex.schema
    // Additional performance indexes for content_versions
    .alterTable('content_versions', function(table) {
      table.index(['status', 'created_at'], 'idx_content_versions_status_created');
      table.index(['lesson_id', 'status'], 'idx_content_versions_lesson_status');
      table.index(['created_by', 'created_at'], 'idx_content_versions_creator_date');
      table.index(['reviewed_by', 'reviewed_at'], 'idx_content_versions_reviewer_date');
    })
    
    // Additional performance indexes for media_files
    .alterTable('media_files', function(table) {
      table.index(['media_type', 'access_level'], 'idx_media_files_type_access');
      table.index(['uploaded_by', 'created_at'], 'idx_media_files_uploader_date');
      table.index(['is_public', 'access_level'], 'idx_media_files_public_access');
      table.index(['file_size_bytes'], 'idx_media_files_size');
    })
    
    // Additional performance indexes for content_approvals
    .alterTable('content_approvals', function(table) {
      table.index(['status', 'requested_at'], 'idx_content_approvals_status_date');
      table.index(['assigned_to', 'status'], 'idx_content_approvals_assignee_status');
      table.index(['requested_by', 'requested_at'], 'idx_content_approvals_requester_date');
    })
    
    // Performance indexes for content_analytics
    .alterTable('content_analytics', function(table) {
      table.index(['content_version_id', 'event_timestamp'], 'idx_content_analytics_content_time');
      table.index(['classroom_id', 'event_timestamp'], 'idx_content_analytics_classroom_time');
      table.index(['event_type', 'event_timestamp'], 'idx_content_analytics_event_time');
      table.index(['user_type', 'event_timestamp'], 'idx_content_analytics_user_time');
    })
    
    // Performance indexes for content_categories
    .alterTable('content_categories', function(table) {
      table.index(['parent_id', 'sort_order'], 'idx_content_categories_parent_sort');
      table.index(['is_active', 'sort_order'], 'idx_content_categories_active_sort');
    })
    
    // Performance indexes for content_tags
    .alterTable('content_tags', function(table) {
      table.index(['is_active', 'name'], 'idx_content_tags_active_name');
    });
};

/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
exports.down = function(knex) {
  return knex.schema
    .alterTable('content_versions', function(table) {
      table.dropIndex([], 'idx_content_versions_status_created');
      table.dropIndex([], 'idx_content_versions_lesson_status');
      table.dropIndex([], 'idx_content_versions_creator_date');
      table.dropIndex([], 'idx_content_versions_reviewer_date');
    })
    .alterTable('media_files', function(table) {
      table.dropIndex([], 'idx_media_files_type_access');
      table.dropIndex([], 'idx_media_files_uploader_date');
      table.dropIndex([], 'idx_media_files_public_access');
      table.dropIndex([], 'idx_media_files_size');
    })
    .alterTable('content_approvals', function(table) {
      table.dropIndex([], 'idx_content_approvals_status_date');
      table.dropIndex([], 'idx_content_approvals_assignee_status');
      table.dropIndex([], 'idx_content_approvals_requester_date');
    })
    .alterTable('content_analytics', function(table) {
      table.dropIndex([], 'idx_content_analytics_content_time');
      table.dropIndex([], 'idx_content_analytics_classroom_time');
      table.dropIndex([], 'idx_content_analytics_event_time');
      table.dropIndex([], 'idx_content_analytics_user_time');
    })
    .alterTable('content_categories', function(table) {
      table.dropIndex([], 'idx_content_categories_parent_sort');
      table.dropIndex([], 'idx_content_categories_active_sort');
    })
    .alterTable('content_tags', function(table) {
      table.dropIndex([], 'idx_content_tags_active_name');
    });
};