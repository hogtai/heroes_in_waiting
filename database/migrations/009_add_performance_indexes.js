exports.up = function(knex) {
  return knex.schema.raw(`
    -- Facilitator authentication indexes
    CREATE INDEX IF NOT EXISTS idx_facilitators_email_active ON facilitators(email, is_active);
    CREATE INDEX IF NOT EXISTS idx_facilitators_organization ON facilitators(organization);
    
    -- Classroom management indexes
    CREATE INDEX IF NOT EXISTS idx_classrooms_facilitator_id ON classrooms(facilitator_id);
    CREATE INDEX IF NOT EXISTS idx_classrooms_code_active ON classrooms(classroom_code, is_active);
    CREATE INDEX IF NOT EXISTS idx_classrooms_created_at ON classrooms(created_at);
    
    -- Student management indexes
    CREATE INDEX IF NOT EXISTS idx_students_classroom_id ON students(classroom_id);
    CREATE INDEX IF NOT EXISTS idx_students_anonymous_id ON students(anonymous_id);
    CREATE INDEX IF NOT EXISTS idx_students_active ON students(is_active);
    CREATE INDEX IF NOT EXISTS idx_students_last_active ON students(last_active_at);
    
    -- Progress tracking indexes
    CREATE INDEX IF NOT EXISTS idx_student_progress_student_lesson ON student_progress(student_id, lesson_id);
    CREATE INDEX IF NOT EXISTS idx_student_progress_completion ON student_progress(completion_status);
    CREATE INDEX IF NOT EXISTS idx_student_progress_updated_at ON student_progress(updated_at);
    
    -- Analytics indexes
    CREATE INDEX IF NOT EXISTS idx_analytics_events_timestamp ON analytics_events(timestamp);
    CREATE INDEX IF NOT EXISTS idx_analytics_events_type_category ON analytics_events(event_type, event_category);
    CREATE INDEX IF NOT EXISTS idx_analytics_events_classroom ON analytics_events(classroom_id);
    
    -- Feedback indexes
    CREATE INDEX IF NOT EXISTS idx_student_feedback_student_lesson ON student_feedback(student_id, lesson_id);
    CREATE INDEX IF NOT EXISTS idx_student_feedback_type ON student_feedback(feedback_type);
    CREATE INDEX IF NOT EXISTS idx_student_feedback_created_at ON student_feedback(created_at);
  `);
};

exports.down = function(knex) {
  return knex.schema.raw(`
    DROP INDEX IF EXISTS idx_facilitators_email_active;
    DROP INDEX IF EXISTS idx_facilitators_organization;
    DROP INDEX IF EXISTS idx_classrooms_facilitator_id;
    DROP INDEX IF EXISTS idx_classrooms_code_active;
    DROP INDEX IF EXISTS idx_classrooms_created_at;
    DROP INDEX IF EXISTS idx_students_classroom_id;
    DROP INDEX IF EXISTS idx_students_anonymous_id;
    DROP INDEX IF EXISTS idx_students_active;
    DROP INDEX IF EXISTS idx_students_last_active;
    DROP INDEX IF EXISTS idx_student_progress_student_lesson;
    DROP INDEX IF EXISTS idx_student_progress_completion;
    DROP INDEX IF EXISTS idx_student_progress_updated_at;
    DROP INDEX IF EXISTS idx_analytics_events_timestamp;
    DROP INDEX IF EXISTS idx_analytics_events_type_category;
    DROP INDEX IF EXISTS idx_analytics_events_classroom;
    DROP INDEX IF EXISTS idx_student_feedback_student_lesson;
    DROP INDEX IF EXISTS idx_student_feedback_type;
    DROP INDEX IF EXISTS idx_student_feedback_created_at;
  `);
}; 