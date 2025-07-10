-- =====================================================
-- Heroes in Waiting - Enhanced Analytics Schema Tests
-- PostgreSQL Database Integrity Tests using pgTAP
-- Date: 2025-07-10
-- =====================================================

-- Load pgTAP testing framework
BEGIN;
SELECT plan(150); -- Expecting 150 tests total

-- =====================================================
-- 1. SCHEMA EXISTENCE TESTS
-- =====================================================

-- Test that all enhanced analytics tables exist
SELECT has_table('behavioral_analytics', 'behavioral_analytics table should exist');
SELECT has_table('lesson_effectiveness', 'lesson_effectiveness table should exist');
SELECT has_table('time_series_analytics', 'time_series_analytics table should exist');
SELECT has_table('educational_impact_metrics', 'educational_impact_metrics table should exist');
SELECT has_table('analytics_aggregation_cache', 'analytics_aggregation_cache table should exist');
SELECT has_table('anonymous_hash_salts', 'anonymous_hash_salts table should exist');

-- Test that archive tables exist for data retention
SELECT has_table('behavioral_analytics_archive', 'behavioral_analytics_archive table should exist');

-- Test that materialized views exist
SELECT has_view('classroom_analytics_dashboard', 'classroom_analytics_dashboard view should exist');
SELECT has_view('facilitator_overview_analytics', 'facilitator_overview_analytics view should exist');

-- =====================================================
-- 2. BEHAVIORAL ANALYTICS TABLE STRUCTURE TESTS
-- =====================================================

-- Test primary key
SELECT has_pk('behavioral_analytics', 'behavioral_analytics should have a primary key');
SELECT col_is_pk('behavioral_analytics', 'id', 'id should be the primary key for behavioral_analytics');

-- Test required columns exist
SELECT has_column('behavioral_analytics', 'id', 'behavioral_analytics should have id column');
SELECT has_column('behavioral_analytics', 'student_session_id', 'behavioral_analytics should have student_session_id column');
SELECT has_column('behavioral_analytics', 'classroom_id', 'behavioral_analytics should have classroom_id column');
SELECT has_column('behavioral_analytics', 'anonymous_student_hash', 'behavioral_analytics should have anonymous_student_hash column');
SELECT has_column('behavioral_analytics', 'behavioral_category', 'behavioral_analytics should have behavioral_category column');
SELECT has_column('behavioral_analytics', 'behavioral_score', 'behavioral_analytics should have behavioral_score column');
SELECT has_column('behavioral_analytics', 'engagement_level', 'behavioral_analytics should have engagement_level column');
SELECT has_column('behavioral_analytics', 'tracked_at', 'behavioral_analytics should have tracked_at column');

-- Test column data types
SELECT col_type_is('behavioral_analytics', 'id', 'uuid', 'id should be UUID type');
SELECT col_type_is('behavioral_analytics', 'student_session_id', 'uuid', 'student_session_id should be UUID type');
SELECT col_type_is('behavioral_analytics', 'classroom_id', 'uuid', 'classroom_id should be UUID type');
SELECT col_type_is('behavioral_analytics', 'anonymous_student_hash', 'character varying(64)', 'anonymous_student_hash should be VARCHAR(64)');
SELECT col_type_is('behavioral_analytics', 'behavioral_category', 'character varying(50)', 'behavioral_category should be VARCHAR(50)');
SELECT col_type_is('behavioral_analytics', 'behavioral_score', 'integer', 'behavioral_score should be INTEGER');
SELECT col_type_is('behavioral_analytics', 'metadata', 'jsonb', 'metadata should be JSONB type');

-- Test NOT NULL constraints
SELECT col_not_null('behavioral_analytics', 'id', 'id should be NOT NULL');
SELECT col_not_null('behavioral_analytics', 'student_session_id', 'student_session_id should be NOT NULL');
SELECT col_not_null('behavioral_analytics', 'classroom_id', 'classroom_id should be NOT NULL');
SELECT col_not_null('behavioral_analytics', 'anonymous_student_hash', 'anonymous_student_hash should be NOT NULL');
SELECT col_not_null('behavioral_analytics', 'behavioral_category', 'behavioral_category should be NOT NULL');

-- Test foreign key constraints
SELECT has_fk('behavioral_analytics', 'behavioral_analytics should have foreign key constraints');
SELECT fk_ok('behavioral_analytics', 'student_session_id', 'student_sessions', 'id', 
             'student_session_id should reference student_sessions.id');
SELECT fk_ok('behavioral_analytics', 'classroom_id', 'classrooms', 'id', 
             'classroom_id should reference classrooms.id');
SELECT fk_ok('behavioral_analytics', 'lesson_id', 'lessons', 'id', 
             'lesson_id should reference lessons.id');

-- Test CHECK constraints for behavioral_category
SELECT col_has_check('behavioral_analytics', 'behavioral_category', 
                     'behavioral_category should have CHECK constraint');

-- Test CHECK constraints for behavioral_score (1-5 range)
SELECT col_has_check('behavioral_analytics', 'behavioral_score', 
                     'behavioral_score should have CHECK constraint');

-- Test CHECK constraints for engagement_level
SELECT col_has_check('behavioral_analytics', 'engagement_level', 
                     'engagement_level should have CHECK constraint');

-- =====================================================
-- 3. LESSON EFFECTIVENESS TABLE STRUCTURE TESTS
-- =====================================================

-- Test primary key and basic structure
SELECT has_pk('lesson_effectiveness', 'lesson_effectiveness should have a primary key');
SELECT col_is_pk('lesson_effectiveness', 'id', 'id should be the primary key for lesson_effectiveness');

-- Test required columns
SELECT has_column('lesson_effectiveness', 'lesson_id', 'lesson_effectiveness should have lesson_id column');
SELECT has_column('lesson_effectiveness', 'classroom_id', 'lesson_effectiveness should have classroom_id column');
SELECT has_column('lesson_effectiveness', 'measurement_date', 'lesson_effectiveness should have measurement_date column');
SELECT has_column('lesson_effectiveness', 'average_engagement_score', 'lesson_effectiveness should have average_engagement_score column');
SELECT has_column('lesson_effectiveness', 'completion_rate', 'lesson_effectiveness should have completion_rate column');
SELECT has_column('lesson_effectiveness', 'pre_assessment_scores', 'lesson_effectiveness should have pre_assessment_scores column');
SELECT has_column('lesson_effectiveness', 'post_assessment_scores', 'lesson_effectiveness should have post_assessment_scores column');

-- Test column data types
SELECT col_type_is('lesson_effectiveness', 'lesson_id', 'uuid', 'lesson_id should be UUID type');
SELECT col_type_is('lesson_effectiveness', 'classroom_id', 'uuid', 'classroom_id should be UUID type');
SELECT col_type_is('lesson_effectiveness', 'measurement_date', 'date', 'measurement_date should be DATE type');
SELECT col_type_is('lesson_effectiveness', 'average_engagement_score', 'numeric(4,2)', 'average_engagement_score should be DECIMAL(4,2)');
SELECT col_type_is('lesson_effectiveness', 'completion_rate', 'numeric(5,2)', 'completion_rate should be DECIMAL(5,2)');
SELECT col_type_is('lesson_effectiveness', 'pre_assessment_scores', 'jsonb', 'pre_assessment_scores should be JSONB');
SELECT col_type_is('lesson_effectiveness', 'post_assessment_scores', 'jsonb', 'post_assessment_scores should be JSONB');

-- Test NOT NULL constraints
SELECT col_not_null('lesson_effectiveness', 'lesson_id', 'lesson_id should be NOT NULL');
SELECT col_not_null('lesson_effectiveness', 'classroom_id', 'classroom_id should be NOT NULL');
SELECT col_not_null('lesson_effectiveness', 'measurement_date', 'measurement_date should be NOT NULL');

-- Test foreign key constraints
SELECT fk_ok('lesson_effectiveness', 'lesson_id', 'lessons', 'id', 
             'lesson_id should reference lessons.id');
SELECT fk_ok('lesson_effectiveness', 'classroom_id', 'classrooms', 'id', 
             'classroom_id should reference classrooms.id');

-- Test CHECK constraints for score ranges
SELECT col_has_check('lesson_effectiveness', 'average_engagement_score', 
                     'average_engagement_score should have CHECK constraint');
SELECT col_has_check('lesson_effectiveness', 'completion_rate', 
                     'completion_rate should have CHECK constraint');
SELECT col_has_check('lesson_effectiveness', 'facilitator_rating', 
                     'facilitator_rating should have CHECK constraint');

-- =====================================================
-- 4. TIME SERIES ANALYTICS TABLE STRUCTURE TESTS
-- =====================================================

-- Test primary key and basic structure
SELECT has_pk('time_series_analytics', 'time_series_analytics should have a primary key');
SELECT col_is_pk('time_series_analytics', 'id', 'id should be the primary key for time_series_analytics');

-- Test required columns
SELECT has_column('time_series_analytics', 'measurement_timestamp', 'time_series_analytics should have measurement_timestamp column');
SELECT has_column('time_series_analytics', 'time_bucket', 'time_series_analytics should have time_bucket column');
SELECT has_column('time_series_analytics', 'student_count', 'time_series_analytics should have student_count column');
SELECT has_column('time_series_analytics', 'total_interactions', 'time_series_analytics should have total_interactions column');
SELECT has_column('time_series_analytics', 'average_engagement_score', 'time_series_analytics should have average_engagement_score column');

-- Test column data types
SELECT col_type_is('time_series_analytics', 'measurement_timestamp', 'timestamp without time zone', 'measurement_timestamp should be TIMESTAMP');
SELECT col_type_is('time_series_analytics', 'time_bucket', 'character varying(20)', 'time_bucket should be VARCHAR(20)');
SELECT col_type_is('time_series_analytics', 'student_count', 'integer', 'student_count should be INTEGER');
SELECT col_type_is('time_series_analytics', 'empathy_score_trend', 'jsonb', 'empathy_score_trend should be JSONB');

-- Test NOT NULL constraints
SELECT col_not_null('time_series_analytics', 'measurement_timestamp', 'measurement_timestamp should be NOT NULL');
SELECT col_not_null('time_series_analytics', 'time_bucket', 'time_bucket should be NOT NULL');
SELECT col_not_null('time_series_analytics', 'student_count', 'student_count should be NOT NULL');

-- Test CHECK constraints for time_bucket
SELECT col_has_check('time_series_analytics', 'time_bucket', 
                     'time_bucket should have CHECK constraint');

-- =====================================================
-- 5. EDUCATIONAL IMPACT METRICS TABLE STRUCTURE TESTS
-- =====================================================

-- Test primary key and basic structure
SELECT has_pk('educational_impact_metrics', 'educational_impact_metrics should have a primary key');
SELECT col_is_pk('educational_impact_metrics', 'id', 'id should be the primary key for educational_impact_metrics');

-- Test required columns
SELECT has_column('educational_impact_metrics', 'study_period_start', 'educational_impact_metrics should have study_period_start column');
SELECT has_column('educational_impact_metrics', 'study_period_end', 'educational_impact_metrics should have study_period_end column');
SELECT has_column('educational_impact_metrics', 'classroom_count', 'educational_impact_metrics should have classroom_count column');
SELECT has_column('educational_impact_metrics', 'total_student_count', 'educational_impact_metrics should have total_student_count column');
SELECT has_column('educational_impact_metrics', 'empathy_development_score', 'educational_impact_metrics should have empathy_development_score column');

-- Test column data types
SELECT col_type_is('educational_impact_metrics', 'study_period_start', 'date', 'study_period_start should be DATE');
SELECT col_type_is('educational_impact_metrics', 'study_period_end', 'date', 'study_period_end should be DATE');
SELECT col_type_is('educational_impact_metrics', 'classroom_count', 'integer', 'classroom_count should be INTEGER');
SELECT col_type_is('educational_impact_metrics', 'total_student_count', 'integer', 'total_student_count should be INTEGER');
SELECT col_type_is('educational_impact_metrics', 'empathy_development_score', 'numeric(5,2)', 'empathy_development_score should be DECIMAL(5,2)');

-- Test NOT NULL constraints
SELECT col_not_null('educational_impact_metrics', 'study_period_start', 'study_period_start should be NOT NULL');
SELECT col_not_null('educational_impact_metrics', 'study_period_end', 'study_period_end should be NOT NULL');
SELECT col_not_null('educational_impact_metrics', 'classroom_count', 'classroom_count should be NOT NULL');
SELECT col_not_null('educational_impact_metrics', 'total_student_count', 'total_student_count should be NOT NULL');

-- Test CHECK constraint for benchmark achievement level
SELECT col_has_check('educational_impact_metrics', 'benchmark_achievement_level', 
                     'benchmark_achievement_level should have CHECK constraint');

-- =====================================================
-- 6. ANALYTICS AGGREGATION CACHE TABLE STRUCTURE TESTS
-- =====================================================

-- Test primary key and basic structure
SELECT has_pk('analytics_aggregation_cache', 'analytics_aggregation_cache should have a primary key');
SELECT col_is_pk('analytics_aggregation_cache', 'id', 'id should be the primary key for analytics_aggregation_cache');

-- Test required columns
SELECT has_column('analytics_aggregation_cache', 'cache_key', 'analytics_aggregation_cache should have cache_key column');
SELECT has_column('analytics_aggregation_cache', 'cache_type', 'analytics_aggregation_cache should have cache_type column');
SELECT has_column('analytics_aggregation_cache', 'aggregated_data', 'analytics_aggregation_cache should have aggregated_data column');
SELECT has_column('analytics_aggregation_cache', 'data_start_date', 'analytics_aggregation_cache should have data_start_date column');
SELECT has_column('analytics_aggregation_cache', 'data_end_date', 'analytics_aggregation_cache should have data_end_date column');
SELECT has_column('analytics_aggregation_cache', 'expires_at', 'analytics_aggregation_cache should have expires_at column');

-- Test column data types
SELECT col_type_is('analytics_aggregation_cache', 'cache_key', 'character varying(255)', 'cache_key should be VARCHAR(255)');
SELECT col_type_is('analytics_aggregation_cache', 'cache_type', 'character varying(50)', 'cache_type should be VARCHAR(50)');
SELECT col_type_is('analytics_aggregation_cache', 'aggregated_data', 'jsonb', 'aggregated_data should be JSONB');
SELECT col_type_is('analytics_aggregation_cache', 'data_start_date', 'date', 'data_start_date should be DATE');
SELECT col_type_is('analytics_aggregation_cache', 'data_end_date', 'date', 'data_end_date should be DATE');
SELECT col_type_is('analytics_aggregation_cache', 'expires_at', 'timestamp without time zone', 'expires_at should be TIMESTAMP');

-- Test NOT NULL constraints
SELECT col_not_null('analytics_aggregation_cache', 'cache_key', 'cache_key should be NOT NULL');
SELECT col_not_null('analytics_aggregation_cache', 'cache_type', 'cache_type should be NOT NULL');
SELECT col_not_null('analytics_aggregation_cache', 'aggregated_data', 'aggregated_data should be NOT NULL');
SELECT col_not_null('analytics_aggregation_cache', 'data_start_date', 'data_start_date should be NOT NULL');
SELECT col_not_null('analytics_aggregation_cache', 'data_end_date', 'data_end_date should be NOT NULL');
SELECT col_not_null('analytics_aggregation_cache', 'expires_at', 'expires_at should be NOT NULL');

-- Test unique constraint on cache_key
SELECT col_is_unique('analytics_aggregation_cache', 'cache_key', 'cache_key should be unique');

-- Test CHECK constraints
SELECT col_has_check('analytics_aggregation_cache', 'cache_type', 
                     'cache_type should have CHECK constraint');
SELECT col_has_check('analytics_aggregation_cache', 'aggregation_level', 
                     'aggregation_level should have CHECK constraint');
SELECT col_has_check('analytics_aggregation_cache', 'cache_status', 
                     'cache_status should have CHECK constraint');
SELECT col_has_check('analytics_aggregation_cache', 'cache_priority', 
                     'cache_priority should have CHECK constraint');

-- Test foreign key constraints
SELECT fk_ok('analytics_aggregation_cache', 'facilitator_id', 'facilitators', 'id', 
             'facilitator_id should reference facilitators.id');
SELECT fk_ok('analytics_aggregation_cache', 'classroom_id', 'classrooms', 'id', 
             'classroom_id should reference classrooms.id');
SELECT fk_ok('analytics_aggregation_cache', 'lesson_id', 'lessons', 'id', 
             'lesson_id should reference lessons.id');

-- =====================================================
-- 7. ANONYMOUS HASH SALTS TABLE STRUCTURE TESTS
-- =====================================================

-- Test primary key and basic structure
SELECT has_pk('anonymous_hash_salts', 'anonymous_hash_salts should have a primary key');
SELECT col_is_pk('anonymous_hash_salts', 'id', 'id should be the primary key for anonymous_hash_salts');

-- Test required columns
SELECT has_column('anonymous_hash_salts', 'salt_date', 'anonymous_hash_salts should have salt_date column');
SELECT has_column('anonymous_hash_salts', 'salt_value', 'anonymous_hash_salts should have salt_value column');
SELECT has_column('anonymous_hash_salts', 'is_active', 'anonymous_hash_salts should have is_active column');

-- Test column data types
SELECT col_type_is('anonymous_hash_salts', 'salt_date', 'date', 'salt_date should be DATE');
SELECT col_type_is('anonymous_hash_salts', 'salt_value', 'character varying(64)', 'salt_value should be VARCHAR(64)');
SELECT col_type_is('anonymous_hash_salts', 'is_active', 'boolean', 'is_active should be BOOLEAN');

-- Test NOT NULL constraints
SELECT col_not_null('anonymous_hash_salts', 'salt_date', 'salt_date should be NOT NULL');
SELECT col_not_null('anonymous_hash_salts', 'salt_value', 'salt_value should be NOT NULL');

-- Test unique constraint on salt_date
SELECT col_is_unique('anonymous_hash_salts', 'salt_date', 'salt_date should be unique');

-- =====================================================
-- 8. INDEX EXISTENCE TESTS
-- =====================================================

-- Test indexes on behavioral_analytics
SELECT has_index('behavioral_analytics', 'idx_behavioral_analytics_student_session', 
                 'idx_behavioral_analytics_student_session should exist');
SELECT has_index('behavioral_analytics', 'idx_behavioral_analytics_classroom', 
                 'idx_behavioral_analytics_classroom should exist');
SELECT has_index('behavioral_analytics', 'idx_behavioral_analytics_category', 
                 'idx_behavioral_analytics_category should exist');
SELECT has_index('behavioral_analytics', 'idx_behavioral_analytics_hash', 
                 'idx_behavioral_analytics_hash should exist');

-- Test indexes on lesson_effectiveness
SELECT has_index('lesson_effectiveness', 'idx_lesson_effectiveness_lesson', 
                 'idx_lesson_effectiveness_lesson should exist');
SELECT has_index('lesson_effectiveness', 'idx_lesson_effectiveness_classroom', 
                 'idx_lesson_effectiveness_classroom should exist');
SELECT has_index('lesson_effectiveness', 'idx_lesson_effectiveness_date', 
                 'idx_lesson_effectiveness_date should exist');

-- Test indexes on time_series_analytics
SELECT has_index('time_series_analytics', 'idx_time_series_timestamp', 
                 'idx_time_series_timestamp should exist');
SELECT has_index('time_series_analytics', 'idx_time_series_bucket', 
                 'idx_time_series_bucket should exist');
SELECT has_index('time_series_analytics', 'idx_time_series_classroom', 
                 'idx_time_series_classroom should exist');

-- Test indexes on analytics_aggregation_cache
SELECT has_index('analytics_aggregation_cache', 'idx_analytics_cache_key', 
                 'idx_analytics_cache_key should exist');
SELECT has_index('analytics_aggregation_cache', 'idx_analytics_cache_type', 
                 'idx_analytics_cache_type should exist');
SELECT has_index('analytics_aggregation_cache', 'idx_analytics_cache_status', 
                 'idx_analytics_cache_status should exist');

-- Test indexes on anonymous_hash_salts
SELECT has_index('anonymous_hash_salts', 'idx_hash_salts_date', 
                 'idx_hash_salts_date should exist');

-- =====================================================
-- 9. FUNCTION EXISTENCE TESTS
-- =====================================================

-- Test that anonymous hashing functions exist
SELECT has_function('generate_anonymous_student_hash', 
                   'generate_anonymous_student_hash function should exist');
SELECT has_function('validate_anonymous_hash_consistency', 
                   'validate_anonymous_hash_consistency function should exist');
SELECT has_function('calculate_behavioral_engagement_score', 
                   'calculate_behavioral_engagement_score function should exist');
SELECT has_function('calculate_lesson_effectiveness_score', 
                   'calculate_lesson_effectiveness_score function should exist');
SELECT has_function('execute_coppa_data_retention', 
                   'execute_coppa_data_retention function should exist');

-- =====================================================
-- 10. TRIGGER EXISTENCE TESTS
-- =====================================================

-- Test that update triggers exist
SELECT has_trigger('behavioral_analytics', 'trigger_behavioral_analytics_updated_at', 
                  'behavioral_analytics should have updated_at trigger');
SELECT has_trigger('lesson_effectiveness', 'trigger_lesson_effectiveness_updated_at', 
                  'lesson_effectiveness should have updated_at trigger');
SELECT has_trigger('time_series_analytics', 'trigger_time_series_analytics_updated_at', 
                  'time_series_analytics should have updated_at trigger');
SELECT has_trigger('analytics_aggregation_cache', 'trigger_analytics_aggregation_cache_updated_at', 
                  'analytics_aggregation_cache should have updated_at trigger');

-- Test that analytics refresh trigger exists
SELECT has_trigger('behavioral_analytics', 'trigger_refresh_analytics_views', 
                  'behavioral_analytics should have view refresh trigger');

-- Finish the test plan
SELECT * FROM finish();
ROLLBACK;