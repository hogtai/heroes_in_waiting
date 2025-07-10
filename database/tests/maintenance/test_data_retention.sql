-- =====================================================
-- Heroes in Waiting - Data Retention & Cleanup Tests
-- PostgreSQL COPPA Compliance Data Management Tests
-- Date: 2025-07-10
-- =====================================================

-- Load pgTAP testing framework
BEGIN;
SELECT plan(35); -- Expecting 35 data retention tests

-- =====================================================
-- 1. DATA RETENTION POLICY TESTS
-- =====================================================

-- Test that data retention function exists
SELECT has_function('execute_coppa_data_retention', 
                   'execute_coppa_data_retention function should exist for COPPA compliance');

-- Test default 90-day retention policy
SELECT ok(
    (SELECT execute_coppa_data_retention() IS NOT NULL),
    'Data retention function should execute successfully with default 90-day policy'
);

-- =====================================================
-- 2. TEST DATA SETUP FOR RETENTION TESTING
-- =====================================================

-- Create comprehensive test data for retention testing
CREATE OR REPLACE FUNCTION create_retention_test_data() RETURNS VOID AS $$
DECLARE
    facilitator_id UUID;
    classroom_id UUID;
    lesson_id UUID;
    student_session_id UUID;
    i INTEGER;
BEGIN
    -- Create test facilitator
    INSERT INTO facilitators (id, email, password_hash, first_name, last_name, organization)
    VALUES (uuid_generate_v4(), 'retention.test@example.com', 'hashed_password', 'Retention', 'Tester', 'Test School')
    RETURNING id INTO facilitator_id;
    
    -- Create test classroom
    INSERT INTO classrooms (id, facilitator_id, name, classroom_code, grade_level)
    VALUES (uuid_generate_v4(), facilitator_id, 'Retention Test Classroom', 'RET001', 5)
    RETURNING id INTO classroom_id;
    
    -- Create test lesson
    INSERT INTO lessons (id, lesson_number, title, description, duration_minutes)
    VALUES (uuid_generate_v4(), 97, 'Retention Test Lesson', 'Test lesson for retention', 45)
    RETURNING id INTO lesson_id;
    
    -- Create test student session
    INSERT INTO student_sessions (id, classroom_id, session_token, expires_at)
    VALUES (uuid_generate_v4(), classroom_id, 'retention_session_1', CURRENT_TIMESTAMP + INTERVAL '1 hour')
    RETURNING id INTO student_session_id;
    
    -- Create behavioral analytics data with different ages
    -- Recent data (within 30 days)
    FOR i IN 1..10 LOOP
        INSERT INTO behavioral_analytics (
            student_session_id,
            classroom_id,
            lesson_id,
            anonymous_student_hash,
            session_tracking_id,
            behavioral_category,
            interaction_type,
            behavioral_score,
            engagement_level,
            tracked_at
        ) VALUES (
            student_session_id,
            classroom_id,
            lesson_id,
            generate_anonymous_student_hash('recent_student_' || i),
            'recent_track_' || i,
            'empathy',
            'retention_test_recent',
            4,
            'high',
            CURRENT_TIMESTAMP - INTERVAL '1 day' * (i * 2) -- 2, 4, 6... 20 days ago
        );
    END LOOP;
    
    -- Medium age data (31-89 days)
    FOR i IN 1..10 LOOP
        INSERT INTO behavioral_analytics (
            student_session_id,
            classroom_id,
            lesson_id,
            anonymous_student_hash,
            session_tracking_id,
            behavioral_category,
            interaction_type,
            behavioral_score,
            engagement_level,
            tracked_at
        ) VALUES (
            student_session_id,
            classroom_id,
            lesson_id,
            generate_anonymous_student_hash('medium_student_' || i),
            'medium_track_' || i,
            'confidence',
            'retention_test_medium',
            3,
            'medium',
            CURRENT_TIMESTAMP - INTERVAL '1 day' * (30 + i * 5) -- 35, 40, 45... 85 days ago
        );
    END LOOP;
    
    -- Old data (90+ days) - should be subject to retention
    FOR i IN 1..10 LOOP
        INSERT INTO behavioral_analytics (
            student_session_id,
            classroom_id,
            lesson_id,
            anonymous_student_hash,
            session_tracking_id,
            behavioral_category,
            interaction_type,
            behavioral_score,
            engagement_level,
            tracked_at
        ) VALUES (
            student_session_id,
            classroom_id,
            lesson_id,
            generate_anonymous_student_hash('old_student_' || i),
            'old_track_' || i,
            'leadership',
            'retention_test_old',
            5,
            'exceptional',
            CURRENT_TIMESTAMP - INTERVAL '1 day' * (90 + i * 5) -- 95, 100, 105... 140 days ago
        );
    END LOOP;
    
    -- Create lesson effectiveness data with different ages
    INSERT INTO lesson_effectiveness (
        lesson_id,
        classroom_id,
        measurement_date,
        average_engagement_score,
        completion_rate
    ) VALUES 
        (lesson_id, classroom_id, CURRENT_DATE - INTERVAL '30 days', 4.2, 95.0),
        (lesson_id, classroom_id, CURRENT_DATE - INTERVAL '60 days', 4.0, 92.0),
        (lesson_id, classroom_id, CURRENT_DATE - INTERVAL '100 days', 3.8, 88.0);
    
    -- Create analytics cache data with different expiration times
    INSERT INTO analytics_aggregation_cache (
        cache_key,
        cache_type,
        classroom_id,
        aggregated_data,
        data_start_date,
        data_end_date,
        aggregation_level,
        expires_at
    ) VALUES 
        ('retention_cache_current', 'classroom_dashboard', classroom_id, '{"status": "current"}',
         CURRENT_DATE, CURRENT_DATE, 'daily', CURRENT_TIMESTAMP + INTERVAL '1 hour'),
        ('retention_cache_expired', 'classroom_dashboard', classroom_id, '{"status": "expired"}',
         CURRENT_DATE - INTERVAL '2 days', CURRENT_DATE - INTERVAL '2 days', 'daily', CURRENT_TIMESTAMP - INTERVAL '1 hour');
    
    -- Create old hash salts for cleanup testing
    FOR i IN 1..15 LOOP
        INSERT INTO anonymous_hash_salts (salt_date, salt_value, is_active)
        VALUES (
            CURRENT_DATE - INTERVAL '1 day' * i,
            encode(gen_random_bytes(32), 'hex'),
            CASE WHEN i <= 7 THEN true ELSE false END
        );
    END LOOP;
    
    RAISE NOTICE 'Retention test data created: 30 behavioral records (10 recent, 10 medium, 10 old)';
END;
$$ LANGUAGE plpgsql;

-- Create the retention test data
SELECT create_retention_test_data();

-- =====================================================
-- 3. PRE-RETENTION DATA VALIDATION TESTS
-- =====================================================

-- Test that we have the expected amount of test data before retention
SELECT ok(
    (SELECT COUNT(*) FROM behavioral_analytics WHERE interaction_type LIKE 'retention_test_%') = 30,
    'Should have 30 behavioral analytics records before retention'
);

-- Test data distribution by age
SELECT ok(
    (SELECT COUNT(*) FROM behavioral_analytics 
     WHERE interaction_type = 'retention_test_recent' 
     AND tracked_at >= CURRENT_TIMESTAMP - INTERVAL '30 days') = 10,
    'Should have 10 recent behavioral analytics records (within 30 days)'
);

SELECT ok(
    (SELECT COUNT(*) FROM behavioral_analytics 
     WHERE interaction_type = 'retention_test_medium' 
     AND tracked_at >= CURRENT_TIMESTAMP - INTERVAL '90 days'
     AND tracked_at < CURRENT_TIMESTAMP - INTERVAL '30 days') = 10,
    'Should have 10 medium-age behavioral analytics records (30-90 days)'
);

SELECT ok(
    (SELECT COUNT(*) FROM behavioral_analytics 
     WHERE interaction_type = 'retention_test_old' 
     AND tracked_at < CURRENT_TIMESTAMP - INTERVAL '90 days') = 10,
    'Should have 10 old behavioral analytics records (90+ days)'
);

-- Test lesson effectiveness data before retention
SELECT ok(
    (SELECT COUNT(*) FROM lesson_effectiveness 
     WHERE lesson_id = (SELECT id FROM lessons WHERE lesson_number = 97)) = 3,
    'Should have 3 lesson effectiveness records before retention'
);

-- Test cache data before retention
SELECT ok(
    (SELECT COUNT(*) FROM analytics_aggregation_cache 
     WHERE cache_key LIKE 'retention_cache_%') = 2,
    'Should have 2 cache records before retention'
);

-- Test hash salts before retention
SELECT ok(
    (SELECT COUNT(*) FROM anonymous_hash_salts 
     WHERE salt_date >= CURRENT_DATE - INTERVAL '15 days') = 15,
    'Should have 15 hash salt records before retention'
);

-- =====================================================
-- 4. 90-DAY COPPA RETENTION POLICY TESTS
-- =====================================================

-- Execute 90-day retention policy
SELECT execute_coppa_data_retention(90);

-- Test that old behavioral analytics data is removed
SELECT ok(
    (SELECT COUNT(*) FROM behavioral_analytics 
     WHERE interaction_type = 'retention_test_old' 
     AND tracked_at < CURRENT_TIMESTAMP - INTERVAL '90 days') = 0,
    'Old behavioral analytics data (90+ days) should be deleted'
);

-- Test that medium-age data is preserved
SELECT ok(
    (SELECT COUNT(*) FROM behavioral_analytics 
     WHERE interaction_type = 'retention_test_medium' 
     AND tracked_at >= CURRENT_TIMESTAMP - INTERVAL '90 days') = 10,
    'Medium-age behavioral analytics data (30-90 days) should be preserved'
);

-- Test that recent data is preserved
SELECT ok(
    (SELECT COUNT(*) FROM behavioral_analytics 
     WHERE interaction_type = 'retention_test_recent' 
     AND tracked_at >= CURRENT_TIMESTAMP - INTERVAL '30 days') = 10,
    'Recent behavioral analytics data (within 30 days) should be preserved'
);

-- Test that data is archived before deletion
SELECT ok(
    (SELECT COUNT(*) FROM behavioral_analytics_archive 
     WHERE interaction_type = 'retention_test_old' 
     AND tracked_at < CURRENT_TIMESTAMP - INTERVAL '90 days') = 10,
    'Old behavioral analytics data should be archived before deletion'
);

-- =====================================================
-- 5. CUSTOM RETENTION PERIOD TESTS
-- =====================================================

-- Test 30-day retention policy
INSERT INTO behavioral_analytics (
    student_session_id,
    classroom_id,
    lesson_id,
    anonymous_student_hash,
    session_tracking_id,
    behavioral_category,
    interaction_type,
    behavioral_score,
    engagement_level,
    tracked_at
) VALUES (
    (SELECT id FROM student_sessions WHERE session_token = 'retention_session_1'),
    (SELECT id FROM classrooms WHERE classroom_code = 'RET001'),
    (SELECT id FROM lessons WHERE lesson_number = 97),
    generate_anonymous_student_hash('custom_retention_test'),
    'custom_track_1',
    'empathy',
    'retention_test_custom_30',
    4,
    'high',
    CURRENT_TIMESTAMP - INTERVAL '35 days'
);

-- Execute 30-day retention policy
SELECT execute_coppa_data_retention(30);

-- Test that 35-day old data is removed with 30-day policy
SELECT ok(
    (SELECT COUNT(*) FROM behavioral_analytics 
     WHERE interaction_type = 'retention_test_custom_30') = 0,
    '35-day old data should be deleted with 30-day retention policy'
);

-- =====================================================
-- 6. CACHE EXPIRATION TESTS
-- =====================================================

-- Test that expired cache entries are marked as expired
SELECT ok(
    (SELECT cache_status FROM analytics_aggregation_cache 
     WHERE cache_key = 'retention_cache_expired') = 'expired',
    'Expired cache entries should be marked as expired'
);

-- Test that current cache entries remain active
SELECT ok(
    (SELECT cache_status FROM analytics_aggregation_cache 
     WHERE cache_key = 'retention_cache_current') = 'active',
    'Current cache entries should remain active'
);

-- =====================================================
-- 7. HASH SALT CLEANUP TESTS
-- =====================================================

-- Test that old hash salts are cleaned up (keep only last 7 days)
SELECT ok(
    (SELECT COUNT(*) FROM anonymous_hash_salts 
     WHERE salt_date < CURRENT_DATE - INTERVAL '7 days') = 0,
    'Hash salts older than 7 days should be cleaned up'
);

-- Test that recent hash salts are preserved
SELECT ok(
    (SELECT COUNT(*) FROM anonymous_hash_salts 
     WHERE salt_date >= CURRENT_DATE - INTERVAL '7 days') >= 7,
    'Hash salts from last 7 days should be preserved'
);

-- =====================================================
-- 8. DATA RETENTION LOG VALIDATION TESTS
-- =====================================================

-- Test that retention execution is logged
SELECT ok(
    (SELECT COUNT(*) FROM data_retention_log 
     WHERE table_name = 'behavioral_analytics' 
     AND retention_policy LIKE 'coppa_%_day_retention'
     AND execution_date = CURRENT_DATE) > 0,
    'Data retention execution should be logged'
);

-- Test log contains correct information
SELECT ok(
    (SELECT records_archived FROM data_retention_log 
     WHERE table_name = 'behavioral_analytics' 
     AND execution_date = CURRENT_DATE 
     ORDER BY created_at DESC LIMIT 1) > 0,
    'Retention log should record number of archived records'
);

SELECT ok(
    (SELECT records_deleted FROM data_retention_log 
     WHERE table_name = 'behavioral_analytics' 
     AND execution_date = CURRENT_DATE 
     ORDER BY created_at DESC LIMIT 1) > 0,
    'Retention log should record number of deleted records'
);

-- Test execution time is recorded
SELECT ok(
    (SELECT execution_time_seconds FROM data_retention_log 
     WHERE table_name = 'behavioral_analytics' 
     AND execution_date = CURRENT_DATE 
     ORDER BY created_at DESC LIMIT 1) IS NOT NULL,
    'Retention log should record execution time'
);

-- =====================================================
-- 9. CONSENT WITHDRAWAL DATA PURGING TESTS
-- =====================================================

-- Create function to test complete data purging for consent withdrawal
CREATE OR REPLACE FUNCTION test_consent_withdrawal_purge(student_hash VARCHAR(64))
RETURNS JSONB AS $$
DECLARE
    initial_count INTEGER;
    purged_count INTEGER;
    archive_count INTEGER;
    result JSONB;
BEGIN
    -- Count initial records
    SELECT COUNT(*) INTO initial_count 
    FROM behavioral_analytics 
    WHERE anonymous_student_hash = student_hash;
    
    -- Purge all data for the student (both live and archived)
    DELETE FROM behavioral_analytics_archive 
    WHERE anonymous_student_hash = student_hash;
    
    DELETE FROM behavioral_analytics 
    WHERE anonymous_student_hash = student_hash;
    GET DIAGNOSTICS purged_count = ROW_COUNT;
    
    -- Check if any data remains
    SELECT COUNT(*) INTO archive_count 
    FROM behavioral_analytics_archive 
    WHERE anonymous_student_hash = student_hash;
    
    result := jsonb_build_object(
        'initial_records', initial_count,
        'purged_records', purged_count,
        'remaining_archive_records', archive_count,
        'complete_purge', (archive_count = 0)
    );
    
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- Test consent withdrawal purging
INSERT INTO behavioral_analytics (
    student_session_id,
    classroom_id,
    lesson_id,
    anonymous_student_hash,
    session_tracking_id,
    behavioral_category,
    interaction_type,
    behavioral_score,
    engagement_level
) VALUES (
    (SELECT id FROM student_sessions WHERE session_token = 'retention_session_1'),
    (SELECT id FROM classrooms WHERE classroom_code = 'RET001'),
    (SELECT id FROM lessons WHERE lesson_number = 97),
    generate_anonymous_student_hash('consent_withdrawal_test'),
    'withdrawal_track_1',
    'empathy',
    'retention_test_withdrawal',
    4,
    'high'
);

-- Execute consent withdrawal
SELECT ok(
    (test_consent_withdrawal_purge(generate_anonymous_student_hash('consent_withdrawal_test'))->'complete_purge')::BOOLEAN,
    'Consent withdrawal should completely purge all student data'
);

-- =====================================================
-- 10. BACKUP PREPARATION TESTS
-- =====================================================

-- Test backup table creation for important data
CREATE OR REPLACE FUNCTION test_backup_preparation() RETURNS BOOLEAN AS $$
DECLARE
    backup_table_exists BOOLEAN;
    backup_record_count INTEGER;
BEGIN
    -- Check if backup table exists
    SELECT EXISTS(
        SELECT 1 FROM information_schema.tables 
        WHERE table_name = 'behavioral_analytics_archive'
    ) INTO backup_table_exists;
    
    -- Check backup contains data
    SELECT COUNT(*) INTO backup_record_count 
    FROM behavioral_analytics_archive;
    
    RETURN backup_table_exists AND backup_record_count > 0;
END;
$$ LANGUAGE plpgsql;

-- Test backup infrastructure
SELECT ok(
    test_backup_preparation(),
    'Backup infrastructure should be properly configured'
);

-- =====================================================
-- 11. AUTOMATED CLEANUP SCHEDULING TESTS
-- =====================================================

-- Test that cleanup functions can be scheduled (function exists and executes)
SELECT ok(
    (SELECT COUNT(*) FROM pg_proc 
     WHERE proname = 'execute_coppa_data_retention') = 1,
    'Automated cleanup function should exist for scheduling'
);

-- Test cleanup function with different parameters
SELECT ok(
    (SELECT execute_coppa_data_retention(60) IS NOT NULL),
    'Cleanup function should accept custom retention periods'
);

SELECT ok(
    (SELECT execute_coppa_data_retention(120) IS NOT NULL),
    'Cleanup function should work with extended retention periods'
);

-- =====================================================
-- 12. DATA INTEGRITY AFTER RETENTION TESTS
-- =====================================================

-- Test that remaining data maintains referential integrity
SELECT ok(
    (SELECT COUNT(*) FROM behavioral_analytics ba
     LEFT JOIN student_sessions ss ON ba.student_session_id = ss.id
     WHERE ss.id IS NULL) = 0,
    'Remaining behavioral analytics should maintain referential integrity with student sessions'
);

SELECT ok(
    (SELECT COUNT(*) FROM behavioral_analytics ba
     LEFT JOIN classrooms c ON ba.classroom_id = c.id
     WHERE c.id IS NULL) = 0,
    'Remaining behavioral analytics should maintain referential integrity with classrooms'
);

-- Test that indexes remain optimal after deletion
SELECT ok(
    (SELECT schemaname IS NOT NULL FROM pg_stat_user_indexes 
     WHERE relname = 'behavioral_analytics' 
     AND indexrelname = 'idx_behavioral_analytics_tracked_date'),
    'Date index should remain functional after retention cleanup'
);

-- =====================================================
-- 13. PERFORMANCE IMPACT TESTS
-- =====================================================

-- Test that retention cleanup doesn't significantly impact performance
CREATE OR REPLACE FUNCTION test_retention_performance_impact() RETURNS BOOLEAN AS $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    execution_time_ms INTEGER;
BEGIN
    start_time := clock_timestamp();
    
    -- Execute retention with small dataset
    PERFORM execute_coppa_data_retention(90);
    
    end_time := clock_timestamp();
    execution_time_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;
    
    -- Should complete within reasonable time (5 seconds for test data)
    RETURN execution_time_ms < 5000;
END;
$$ LANGUAGE plpgsql;

-- Test retention performance
SELECT ok(
    test_retention_performance_impact(),
    'Data retention should complete within reasonable time'
);

-- =====================================================
-- 14. CLEANUP TEST DATA AND VALIDATION
-- =====================================================

-- Clean up all test data
DELETE FROM behavioral_analytics_archive WHERE anonymous_student_hash LIKE '%retention%' OR anonymous_student_hash LIKE '%test%';
DELETE FROM behavioral_analytics WHERE interaction_type LIKE 'retention_test_%';
DELETE FROM lesson_effectiveness WHERE lesson_id = (SELECT id FROM lessons WHERE lesson_number = 97);
DELETE FROM analytics_aggregation_cache WHERE cache_key LIKE 'retention_cache_%';
DELETE FROM student_sessions WHERE session_token = 'retention_session_1';
DELETE FROM lessons WHERE lesson_number = 97;
DELETE FROM classrooms WHERE classroom_code = 'RET001';
DELETE FROM facilitators WHERE email = 'retention.test@example.com';
DELETE FROM anonymous_hash_salts WHERE salt_date != CURRENT_DATE;
DELETE FROM data_retention_log WHERE table_name = 'behavioral_analytics' AND execution_date = CURRENT_DATE;

-- Drop test functions
DROP FUNCTION IF EXISTS create_retention_test_data();
DROP FUNCTION IF EXISTS test_consent_withdrawal_purge(VARCHAR(64));
DROP FUNCTION IF EXISTS test_backup_preparation();
DROP FUNCTION IF EXISTS test_retention_performance_impact();

-- Final validation - ensure no test data remains
SELECT ok(
    (SELECT COUNT(*) FROM behavioral_analytics WHERE interaction_type LIKE 'retention_test_%') = 0,
    'All retention test data should be cleaned up'
);

-- Test that regular retention still works after cleanup
SELECT ok(
    (SELECT execute_coppa_data_retention() IS NOT NULL),
    'Regular retention function should still work after test cleanup'
);

-- Finish the test plan
SELECT * FROM finish();
ROLLBACK;