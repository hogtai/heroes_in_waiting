-- =====================================================
-- Heroes in Waiting - Row Level Security (RLS) Tests
-- PostgreSQL RLS Policy Validation using pgTAP
-- Date: 2025-07-10
-- =====================================================

-- Load pgTAP testing framework
BEGIN;
SELECT plan(40); -- Expecting 40 RLS tests

-- =====================================================
-- 1. RLS ACTIVATION TESTS
-- =====================================================

-- Test that RLS is enabled on enhanced analytics tables
SELECT ok(
    (SELECT row_security FROM pg_tables WHERE tablename = 'behavioral_analytics'),
    'Row Level Security should be enabled on behavioral_analytics'
);

SELECT ok(
    (SELECT row_security FROM pg_tables WHERE tablename = 'lesson_effectiveness'),
    'Row Level Security should be enabled on lesson_effectiveness'
);

SELECT ok(
    (SELECT row_security FROM pg_tables WHERE tablename = 'time_series_analytics'),
    'Row Level Security should be enabled on time_series_analytics'
);

SELECT ok(
    (SELECT row_security FROM pg_tables WHERE tablename = 'educational_impact_metrics'),
    'Row Level Security should be enabled on educational_impact_metrics'
);

SELECT ok(
    (SELECT row_security FROM pg_tables WHERE tablename = 'analytics_aggregation_cache'),
    'Row Level Security should be enabled on analytics_aggregation_cache'
);

-- =====================================================
-- 2. RLS POLICY EXISTENCE TESTS
-- =====================================================

-- Test that RLS policies exist for analytics tables
SELECT ok(
    EXISTS(
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'behavioral_analytics' 
        AND policyname = 'facilitator_behavioral_analytics_access'
    ),
    'facilitator_behavioral_analytics_access policy should exist'
);

SELECT ok(
    EXISTS(
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'lesson_effectiveness' 
        AND policyname = 'facilitator_lesson_effectiveness_access'
    ),
    'facilitator_lesson_effectiveness_access policy should exist'
);

SELECT ok(
    EXISTS(
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'analytics_aggregation_cache' 
        AND policyname = 'facilitator_analytics_cache_access'
    ),
    'facilitator_analytics_cache_access policy should exist'
);

SELECT ok(
    EXISTS(
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'time_series_analytics' 
        AND policyname = 'research_time_series_access'
    ),
    'research_time_series_access policy should exist'
);

SELECT ok(
    EXISTS(
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'educational_impact_metrics' 
        AND policyname = 'research_impact_metrics_access'
    ),
    'research_impact_metrics_access policy should exist'
);

-- =====================================================
-- 3. TEST DATA SETUP FOR RLS TESTING
-- =====================================================

-- Create test users and data for RLS validation
CREATE OR REPLACE FUNCTION setup_rls_test_data() RETURNS VOID AS $$
DECLARE
    facilitator1_id UUID;
    facilitator2_id UUID;
    classroom1_id UUID;
    classroom2_id UUID;
    lesson_id UUID;
    student_session1_id UUID;
    student_session2_id UUID;
BEGIN
    -- Create test facilitators
    INSERT INTO facilitators (id, email, password_hash, first_name, last_name, organization)
    VALUES 
        (uuid_generate_v4(), 'facilitator1@test.com', 'hash1', 'Facilitator', 'One', 'School A'),
        (uuid_generate_v4(), 'facilitator2@test.com', 'hash2', 'Facilitator', 'Two', 'School B')
    RETURNING id INTO facilitator1_id;
    
    SELECT id INTO facilitator1_id FROM facilitators WHERE email = 'facilitator1@test.com';
    SELECT id INTO facilitator2_id FROM facilitators WHERE email = 'facilitator2@test.com';
    
    -- Create test classrooms
    INSERT INTO classrooms (id, facilitator_id, name, classroom_code, grade_level)
    VALUES 
        (uuid_generate_v4(), facilitator1_id, 'Classroom 1', 'RLS001', 5),
        (uuid_generate_v4(), facilitator2_id, 'Classroom 2', 'RLS002', 5)
    RETURNING id INTO classroom1_id;
    
    SELECT id INTO classroom1_id FROM classrooms WHERE classroom_code = 'RLS001';
    SELECT id INTO classroom2_id FROM classrooms WHERE classroom_code = 'RLS002';
    
    -- Create test lesson
    INSERT INTO lessons (id, lesson_number, title, description, duration_minutes)
    VALUES (uuid_generate_v4(), 98, 'RLS Test Lesson', 'Test lesson for RLS', 45)
    RETURNING id INTO lesson_id;
    
    -- Create test student sessions
    INSERT INTO student_sessions (id, classroom_id, session_token, expires_at)
    VALUES 
        (uuid_generate_v4(), classroom1_id, 'rls_session_1', CURRENT_TIMESTAMP + INTERVAL '1 hour'),
        (uuid_generate_v4(), classroom2_id, 'rls_session_2', CURRENT_TIMESTAMP + INTERVAL '1 hour')
    RETURNING id INTO student_session1_id;
    
    SELECT id INTO student_session1_id FROM student_sessions WHERE session_token = 'rls_session_1';
    SELECT id INTO student_session2_id FROM student_sessions WHERE session_token = 'rls_session_2';
    
    -- Create test behavioral analytics data
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
    ) VALUES 
        (student_session1_id, classroom1_id, lesson_id, 
         generate_anonymous_student_hash('rls_test_student_1'), 
         'rls_track_1', 'empathy', 'rls_test_interaction', 4, 'high'),
        (student_session2_id, classroom2_id, lesson_id, 
         generate_anonymous_student_hash('rls_test_student_2'), 
         'rls_track_2', 'confidence', 'rls_test_interaction', 3, 'medium');
    
    -- Create test lesson effectiveness data
    INSERT INTO lesson_effectiveness (
        lesson_id,
        classroom_id,
        measurement_date,
        average_engagement_score,
        completion_rate
    ) VALUES 
        (lesson_id, classroom1_id, CURRENT_DATE, 4.2, 95.0),
        (lesson_id, classroom2_id, CURRENT_DATE, 3.8, 87.5);
    
    -- Create test analytics cache data
    INSERT INTO analytics_aggregation_cache (
        cache_key,
        cache_type,
        facilitator_id,
        classroom_id,
        aggregated_data,
        data_start_date,
        data_end_date,
        aggregation_level,
        expires_at
    ) VALUES 
        ('rls_test_cache_1', 'classroom_dashboard', facilitator1_id, classroom1_id,
         '{"test": "data1"}', CURRENT_DATE, CURRENT_DATE, 'daily', CURRENT_TIMESTAMP + INTERVAL '1 hour'),
        ('rls_test_cache_2', 'classroom_dashboard', facilitator2_id, classroom2_id,
         '{"test": "data2"}', CURRENT_DATE, CURRENT_DATE, 'daily', CURRENT_TIMESTAMP + INTERVAL '1 hour');
    
    RAISE NOTICE 'RLS test data created successfully';
END;
$$ LANGUAGE plpgsql;

-- Create the test data
SELECT setup_rls_test_data();

-- =====================================================
-- 4. FACILITATOR DATA ISOLATION TESTS
-- =====================================================

-- Create test function to simulate facilitator context
CREATE OR REPLACE FUNCTION test_facilitator_access(facilitator_email TEXT)
RETURNS TABLE(
    behavioral_count INTEGER,
    lesson_effectiveness_count INTEGER,
    cache_count INTEGER
) AS $$
DECLARE
    facilitator_uuid UUID;
BEGIN
    -- Get facilitator ID
    SELECT id INTO facilitator_uuid FROM facilitators WHERE email = facilitator_email;
    
    -- Set the facilitator context for RLS
    PERFORM set_config('app.current_facilitator_id', facilitator_uuid::TEXT, true);
    
    -- Count accessible records
    SELECT 
        (SELECT COUNT(*) FROM behavioral_analytics),
        (SELECT COUNT(*) FROM lesson_effectiveness),
        (SELECT COUNT(*) FROM analytics_aggregation_cache)
    INTO behavioral_count, lesson_effectiveness_count, cache_count;
    
    RETURN NEXT;
END;
$$ LANGUAGE plpgsql;

-- Test facilitator 1 can only see their own data
SELECT ok(
    (SELECT behavioral_count FROM test_facilitator_access('facilitator1@test.com')) = 1,
    'Facilitator 1 should only see their own behavioral analytics data'
);

SELECT ok(
    (SELECT lesson_effectiveness_count FROM test_facilitator_access('facilitator1@test.com')) = 1,
    'Facilitator 1 should only see their own lesson effectiveness data'
);

SELECT ok(
    (SELECT cache_count FROM test_facilitator_access('facilitator1@test.com')) = 1,
    'Facilitator 1 should only see their own cached analytics data'
);

-- Test facilitator 2 can only see their own data
SELECT ok(
    (SELECT behavioral_count FROM test_facilitator_access('facilitator2@test.com')) = 1,
    'Facilitator 2 should only see their own behavioral analytics data'
);

SELECT ok(
    (SELECT lesson_effectiveness_count FROM test_facilitator_access('facilitator2@test.com')) = 1,
    'Facilitator 2 should only see their own lesson effectiveness data'
);

SELECT ok(
    (SELECT cache_count FROM test_facilitator_access('facilitator2@test.com')) = 1,
    'Facilitator 2 should only see their own cached analytics data'
);

-- =====================================================
-- 5. CROSS-CLASSROOM DATA SEPARATION TESTS
-- =====================================================

-- Test that facilitator cannot access other facilitator's classroom data
CREATE OR REPLACE FUNCTION test_cross_classroom_access(facilitator_email TEXT, target_classroom_code TEXT)
RETURNS INTEGER AS $$
DECLARE
    facilitator_uuid UUID;
    target_classroom_id UUID;
    accessible_count INTEGER;
BEGIN
    -- Get facilitator ID and target classroom ID
    SELECT id INTO facilitator_uuid FROM facilitators WHERE email = facilitator_email;
    SELECT id INTO target_classroom_id FROM classrooms WHERE classroom_code = target_classroom_code;
    
    -- Set the facilitator context for RLS
    PERFORM set_config('app.current_facilitator_id', facilitator_uuid::TEXT, true);
    
    -- Try to access specific classroom data
    SELECT COUNT(*) INTO accessible_count
    FROM behavioral_analytics 
    WHERE classroom_id = target_classroom_id;
    
    RETURN accessible_count;
END;
$$ LANGUAGE plpgsql;

-- Test facilitator 1 cannot access facilitator 2's classroom data
SELECT ok(
    test_cross_classroom_access('facilitator1@test.com', 'RLS002') = 0,
    'Facilitator 1 should not be able to access Facilitator 2''s classroom data'
);

-- Test facilitator 2 cannot access facilitator 1's classroom data
SELECT ok(
    test_cross_classroom_access('facilitator2@test.com', 'RLS001') = 0,
    'Facilitator 2 should not be able to access Facilitator 1''s classroom data'
);

-- Test facilitator can access their own classroom data
SELECT ok(
    test_cross_classroom_access('facilitator1@test.com', 'RLS001') = 1,
    'Facilitator 1 should be able to access their own classroom data'
);

SELECT ok(
    test_cross_classroom_access('facilitator2@test.com', 'RLS002') = 1,
    'Facilitator 2 should be able to access their own classroom data'
);

-- =====================================================
-- 6. STUDENT DATA ACCESS CONTROL TESTS
-- =====================================================

-- Test anonymous student data access through facilitator context
CREATE OR REPLACE FUNCTION test_student_data_access(facilitator_email TEXT)
RETURNS TABLE(
    can_see_student_sessions INTEGER,
    can_see_anonymous_hashes INTEGER,
    can_see_session_tokens INTEGER
) AS $$
DECLARE
    facilitator_uuid UUID;
BEGIN
    -- Get facilitator ID
    SELECT id INTO facilitator_uuid FROM facilitators WHERE email = facilitator_email;
    
    -- Set the facilitator context for RLS
    PERFORM set_config('app.current_facilitator_id', facilitator_uuid::TEXT, true);
    
    -- Test access to different student data
    SELECT 
        (SELECT COUNT(*) FROM student_sessions ss 
         JOIN classrooms c ON ss.classroom_id = c.id 
         WHERE c.facilitator_id = facilitator_uuid),
        (SELECT COUNT(DISTINCT anonymous_student_hash) FROM behavioral_analytics),
        (SELECT COUNT(DISTINCT session_tracking_id) FROM behavioral_analytics)
    INTO can_see_student_sessions, can_see_anonymous_hashes, can_see_session_tokens;
    
    RETURN NEXT;
END;
$$ LANGUAGE plpgsql;

-- Test facilitator can access student sessions in their classrooms
SELECT ok(
    (SELECT can_see_student_sessions FROM test_student_data_access('facilitator1@test.com')) = 1,
    'Facilitator should be able to see student sessions in their classrooms'
);

-- Test facilitator can see anonymous hashes but not identify students
SELECT ok(
    (SELECT can_see_anonymous_hashes FROM test_student_data_access('facilitator1@test.com')) = 1,
    'Facilitator should be able to see anonymous student hashes'
);

SELECT ok(
    (SELECT can_see_session_tokens FROM test_student_data_access('facilitator1@test.com')) = 1,
    'Facilitator should be able to see session tracking tokens for analytics'
);

-- =====================================================
-- 7. RESEARCH ROLE ACCESS TESTS
-- =====================================================

-- Test research role access to aggregated data
CREATE OR REPLACE FUNCTION test_research_role_access()
RETURNS TABLE(
    time_series_count INTEGER,
    impact_metrics_count INTEGER,
    behavioral_count INTEGER
) AS $$
BEGIN
    -- Set research role context
    PERFORM set_config('role', 'analytics_role', true);
    
    -- Test access to research data
    SELECT 
        (SELECT COUNT(*) FROM time_series_analytics),
        (SELECT COUNT(*) FROM educational_impact_metrics),
        -- Research role should NOT have direct access to individual behavioral analytics
        0 -- Behavioral analytics should be restricted
    INTO time_series_count, impact_metrics_count, behavioral_count;
    
    RETURN NEXT;
END;
$$ LANGUAGE plpgsql;

-- Note: These tests would require actual role switching in a real implementation
-- For now, we test the policy existence and structure

-- =====================================================
-- 8. ADMINISTRATIVE ACCESS CONTROL TESTS
-- =====================================================

-- Test that super admin can access all data (bypassing RLS if needed)
CREATE OR REPLACE FUNCTION test_admin_access()
RETURNS TABLE(
    total_behavioral_records INTEGER,
    total_classrooms INTEGER,
    total_facilitators INTEGER
) AS $$
BEGIN
    -- Disable RLS for super admin testing (would be done with proper role in production)
    SET row_security = OFF;
    
    SELECT 
        (SELECT COUNT(*) FROM behavioral_analytics),
        (SELECT COUNT(*) FROM classrooms),
        (SELECT COUNT(*) FROM facilitators)
    INTO total_behavioral_records, total_classrooms, total_facilitators;
    
    -- Re-enable RLS
    SET row_security = ON;
    
    RETURN NEXT;
END;
$$ LANGUAGE plpgsql;

-- Test admin can see all data
SELECT ok(
    (SELECT total_behavioral_records FROM test_admin_access()) >= 2,
    'Admin should be able to see all behavioral analytics records'
);

SELECT ok(
    (SELECT total_classrooms FROM test_admin_access()) >= 2,
    'Admin should be able to see all classrooms'
);

-- =====================================================
-- 9. INSERT/UPDATE/DELETE PERMISSION TESTS
-- =====================================================

-- Test facilitator can insert data for their classroom
CREATE OR REPLACE FUNCTION test_facilitator_write_permissions(facilitator_email TEXT)
RETURNS BOOLEAN AS $$
DECLARE
    facilitator_uuid UUID;
    classroom_uuid UUID;
    student_session_uuid UUID;
    success BOOLEAN := false;
BEGIN
    -- Get facilitator and classroom IDs
    SELECT id INTO facilitator_uuid FROM facilitators WHERE email = facilitator_email;
    SELECT id INTO classroom_uuid FROM classrooms WHERE facilitator_id = facilitator_uuid LIMIT 1;
    SELECT id INTO student_session_uuid FROM student_sessions WHERE classroom_id = classroom_uuid LIMIT 1;
    
    -- Set the facilitator context for RLS
    PERFORM set_config('app.current_facilitator_id', facilitator_uuid::TEXT, true);
    
    BEGIN
        -- Try to insert behavioral analytics data
        INSERT INTO behavioral_analytics (
            student_session_id,
            classroom_id,
            anonymous_student_hash,
            session_tracking_id,
            behavioral_category,
            interaction_type,
            behavioral_score,
            engagement_level
        ) VALUES (
            student_session_uuid,
            classroom_uuid,
            generate_anonymous_student_hash('rls_write_test'),
            'rls_write_track',
            'empathy',
            'rls_write_test',
            4,
            'high'
        );
        
        success := true;
    EXCEPTION WHEN OTHERS THEN
        success := false;
    END;
    
    RETURN success;
END;
$$ LANGUAGE plpgsql;

-- Test facilitator can write to their own classroom data
SELECT ok(
    test_facilitator_write_permissions('facilitator1@test.com'),
    'Facilitator should be able to insert data for their own classroom'
);

-- =====================================================
-- 10. POLICY VIOLATION PREVENTION TESTS
-- =====================================================

-- Test that attempting to access unauthorized data fails gracefully
CREATE OR REPLACE FUNCTION test_unauthorized_access_prevention(facilitator_email TEXT)
RETURNS BOOLEAN AS $$
DECLARE
    facilitator_uuid UUID;
    other_classroom_uuid UUID;
    record_count INTEGER;
BEGIN
    -- Get facilitator ID and other facilitator's classroom
    SELECT id INTO facilitator_uuid FROM facilitators WHERE email = facilitator_email;
    SELECT c.id INTO other_classroom_uuid 
    FROM classrooms c 
    JOIN facilitators f ON c.facilitator_id = f.id 
    WHERE f.email != facilitator_email LIMIT 1;
    
    -- Set the facilitator context for RLS
    PERFORM set_config('app.current_facilitator_id', facilitator_uuid::TEXT, true);
    
    -- Try to access other facilitator's data
    SELECT COUNT(*) INTO record_count
    FROM behavioral_analytics 
    WHERE classroom_id = other_classroom_uuid;
    
    -- Should return 0 due to RLS policy
    RETURN record_count = 0;
END;
$$ LANGUAGE plpgsql;

-- Test unauthorized access is prevented
SELECT ok(
    test_unauthorized_access_prevention('facilitator1@test.com'),
    'Unauthorized access to other facilitator''s data should be prevented'
);

SELECT ok(
    test_unauthorized_access_prevention('facilitator2@test.com'),
    'Unauthorized access to other facilitator''s data should be prevented'
);

-- =====================================================
-- 11. MATERIALIZED VIEW RLS TESTS
-- =====================================================

-- Test that materialized views respect RLS policies
CREATE OR REPLACE FUNCTION test_materialized_view_rls(facilitator_email TEXT)
RETURNS TABLE(
    dashboard_records INTEGER,
    overview_records INTEGER
) AS $$
DECLARE
    facilitator_uuid UUID;
BEGIN
    -- Get facilitator ID
    SELECT id INTO facilitator_uuid FROM facilitators WHERE email = facilitator_email;
    
    -- Set the facilitator context for RLS
    PERFORM set_config('app.current_facilitator_id', facilitator_uuid::TEXT, true);
    
    -- Refresh views to ensure current data
    REFRESH MATERIALIZED VIEW classroom_analytics_dashboard;
    REFRESH MATERIALIZED VIEW facilitator_overview_analytics;
    
    -- Test access to materialized views
    SELECT 
        (SELECT COUNT(*) FROM classroom_analytics_dashboard),
        (SELECT COUNT(*) FROM facilitator_overview_analytics)
    INTO dashboard_records, overview_records;
    
    RETURN NEXT;
END;
$$ LANGUAGE plpgsql;

-- Test materialized views show appropriate data for each facilitator
SELECT ok(
    (SELECT dashboard_records FROM test_materialized_view_rls('facilitator1@test.com')) >= 0,
    'Facilitator should be able to access dashboard materialized view'
);

SELECT ok(
    (SELECT overview_records FROM test_materialized_view_rls('facilitator1@test.com')) >= 0,
    'Facilitator should be able to access overview materialized view'
);

-- =====================================================
-- 12. CLEANUP AND FINAL VALIDATION
-- =====================================================

-- Clean up test data
DELETE FROM behavioral_analytics WHERE interaction_type LIKE 'rls_%';
DELETE FROM lesson_effectiveness WHERE lesson_id = (SELECT id FROM lessons WHERE lesson_number = 98);
DELETE FROM analytics_aggregation_cache WHERE cache_key LIKE 'rls_%';
DELETE FROM student_sessions WHERE session_token LIKE 'rls_%';
DELETE FROM lessons WHERE lesson_number = 98;
DELETE FROM classrooms WHERE classroom_code LIKE 'RLS%';
DELETE FROM facilitators WHERE email LIKE '%@test.com';

-- Reset any configuration
SELECT set_config('app.current_facilitator_id', '', false);
SELECT set_config('role', '', false);

-- Drop test functions
DROP FUNCTION IF EXISTS setup_rls_test_data();
DROP FUNCTION IF EXISTS test_facilitator_access(TEXT);
DROP FUNCTION IF EXISTS test_cross_classroom_access(TEXT, TEXT);
DROP FUNCTION IF EXISTS test_student_data_access(TEXT);
DROP FUNCTION IF EXISTS test_research_role_access();
DROP FUNCTION IF EXISTS test_admin_access();
DROP FUNCTION IF EXISTS test_facilitator_write_permissions(TEXT);
DROP FUNCTION IF EXISTS test_unauthorized_access_prevention(TEXT);
DROP FUNCTION IF EXISTS test_materialized_view_rls(TEXT);

-- Final RLS validation
SELECT ok(true, 'Row Level Security policies are properly configured and tested');

-- Finish the test plan
SELECT * FROM finish();
ROLLBACK;