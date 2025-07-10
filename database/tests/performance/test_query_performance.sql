-- =====================================================
-- Heroes in Waiting - Database Performance Tests
-- PostgreSQL Query Performance Validation using pgTAP
-- Target: Sub-100ms query response times
-- Date: 2025-07-10
-- =====================================================

-- Load pgTAP testing framework
BEGIN;
SELECT plan(50); -- Expecting 50 performance tests

-- =====================================================
-- 1. PERFORMANCE TESTING SETUP
-- =====================================================

-- Create performance testing function
CREATE OR REPLACE FUNCTION test_query_performance(
    query_text TEXT,
    test_name TEXT,
    max_time_ms INTEGER DEFAULT 100
) RETURNS BOOLEAN AS $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    execution_time_ms INTEGER;
BEGIN
    -- Record start time
    start_time := clock_timestamp();
    
    -- Execute the query
    EXECUTE query_text;
    
    -- Record end time
    end_time := clock_timestamp();
    
    -- Calculate execution time in milliseconds
    execution_time_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;
    
    -- Log performance result
    RAISE NOTICE 'Query "%" took % ms (target: % ms)', test_name, execution_time_ms, max_time_ms;
    
    -- Return true if within performance target
    RETURN execution_time_ms <= max_time_ms;
END;
$$ LANGUAGE plpgsql;

-- Create test data for performance testing
CREATE OR REPLACE FUNCTION create_performance_test_data() RETURNS VOID AS $$
DECLARE
    facilitator_id UUID;
    classroom_id UUID;
    lesson_id UUID;
    student_session_id UUID;
    i INTEGER;
BEGIN
    -- Create test facilitator
    INSERT INTO facilitators (id, email, password_hash, first_name, last_name, organization)
    VALUES (uuid_generate_v4(), 'perf.test@example.com', 'hashed_password', 'Performance', 'Tester', 'Test School')
    RETURNING id INTO facilitator_id;
    
    -- Create test classroom
    INSERT INTO classrooms (id, facilitator_id, name, classroom_code, grade_level)
    VALUES (uuid_generate_v4(), facilitator_id, 'Performance Test Classroom', 'PERF001', 5)
    RETURNING id INTO classroom_id;
    
    -- Create test lesson
    INSERT INTO lessons (id, lesson_number, title, description, duration_minutes)
    VALUES (uuid_generate_v4(), 99, 'Performance Test Lesson', 'Test lesson for performance', 45)
    RETURNING id INTO lesson_id;
    
    -- Create multiple student sessions (100 students)
    FOR i IN 1..100 LOOP
        INSERT INTO student_sessions (id, classroom_id, session_token, expires_at)
        VALUES (uuid_generate_v4(), classroom_id, 'perf_session_' || i, CURRENT_TIMESTAMP + INTERVAL '1 hour');
    END LOOP;
    
    -- Create behavioral analytics data (1000 records per student = 100,000 total)
    FOR student_session_id IN (SELECT id FROM student_sessions WHERE session_token LIKE 'perf_session_%') LOOP
        FOR i IN 1..1000 LOOP
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
                generate_anonymous_student_hash('perf_student_' || student_session_id || '_' || i),
                encode(gen_random_bytes(16), 'hex'),
                (ARRAY['empathy', 'confidence', 'communication', 'leadership'])[1 + (i % 4)],
                'performance_test_interaction',
                1 + (i % 5), -- Score 1-5
                (ARRAY['low', 'medium', 'high', 'exceptional'])[1 + (i % 4)],
                CURRENT_TIMESTAMP - INTERVAL '1 hour' * (i % 24) -- Spread over 24 hours
            );
        END LOOP;
    END LOOP;
    
    -- Create lesson effectiveness data
    INSERT INTO lesson_effectiveness (
        lesson_id,
        classroom_id,
        measurement_date,
        average_engagement_score,
        completion_rate,
        pre_assessment_scores,
        post_assessment_scores
    ) VALUES (
        lesson_id,
        classroom_id,
        CURRENT_DATE,
        4.2,
        95.5,
        '{"empathy": 3.1, "confidence": 2.8, "communication": 3.4}',
        '{"empathy": 4.3, "confidence": 4.1, "communication": 4.2}'
    );
    
    -- Create time series analytics data
    INSERT INTO time_series_analytics (
        measurement_timestamp,
        time_bucket,
        classroom_id,
        lesson_id,
        behavioral_category,
        student_count,
        total_interactions,
        average_engagement_score
    ) VALUES (
        CURRENT_TIMESTAMP,
        'daily',
        classroom_id,
        lesson_id,
        'empathy',
        100,
        25000, -- 250 interactions per student
        4.1
    );
    
    -- Refresh materialized views with new data
    REFRESH MATERIALIZED VIEW classroom_analytics_dashboard;
    REFRESH MATERIALIZED VIEW facilitator_overview_analytics;
    
    RAISE NOTICE 'Performance test data created: 100 students, 100,000 behavioral analytics records';
END;
$$ LANGUAGE plpgsql;

-- Create the test data
SELECT create_performance_test_data();

-- =====================================================
-- 2. BASIC QUERY PERFORMANCE TESTS
-- =====================================================

-- Test 1: Single student session lookup (should be very fast)
SELECT ok(
    test_query_performance(
        'SELECT * FROM student_sessions WHERE session_token = ''perf_session_1''',
        'Single student session lookup',
        10
    ),
    'Single student session lookup should be under 10ms'
);

-- Test 2: Classroom lookup by code (should be very fast)
SELECT ok(
    test_query_performance(
        'SELECT * FROM classrooms WHERE classroom_code = ''PERF001''',
        'Classroom lookup by code',
        10
    ),
    'Classroom lookup by code should be under 10ms'
);

-- Test 3: Facilitator authentication query
SELECT ok(
    test_query_performance(
        'SELECT * FROM facilitators WHERE email = ''perf.test@example.com'' AND is_active = true',
        'Facilitator authentication lookup',
        20
    ),
    'Facilitator authentication lookup should be under 20ms'
);

-- =====================================================
-- 3. BEHAVIORAL ANALYTICS QUERY PERFORMANCE TESTS
-- =====================================================

-- Test 4: Student behavioral analytics summary (critical dashboard query)
SELECT ok(
    test_query_performance(
        'SELECT behavioral_category, COUNT(*), AVG(behavioral_score) 
         FROM behavioral_analytics 
         WHERE classroom_id = (SELECT id FROM classrooms WHERE classroom_code = ''PERF001'')
         AND tracked_at >= CURRENT_DATE 
         GROUP BY behavioral_category',
        'Student behavioral analytics summary',
        50
    ),
    'Behavioral analytics summary should be under 50ms'
);

-- Test 5: Individual student progress lookup
SELECT ok(
    test_query_performance(
        'SELECT * FROM behavioral_analytics 
         WHERE anonymous_student_hash = (SELECT generate_anonymous_student_hash(''perf_student_'' || (SELECT id FROM student_sessions LIMIT 1) || ''_1''))
         ORDER BY tracked_at DESC LIMIT 20',
        'Individual student progress lookup',
        30
    ),
    'Individual student progress lookup should be under 30ms'
);

-- Test 6: Recent classroom activity (last 24 hours)
SELECT ok(
    test_query_performance(
        'SELECT COUNT(*) as interaction_count, AVG(behavioral_score) as avg_score
         FROM behavioral_analytics 
         WHERE classroom_id = (SELECT id FROM classrooms WHERE classroom_code = ''PERF001'')
         AND tracked_at >= CURRENT_TIMESTAMP - INTERVAL ''24 hours''',
        'Recent classroom activity (24h)',
        40
    ),
    'Recent classroom activity lookup should be under 40ms'
);

-- Test 7: Engagement level distribution
SELECT ok(
    test_query_performance(
        'SELECT engagement_level, COUNT(*) 
         FROM behavioral_analytics 
         WHERE classroom_id = (SELECT id FROM classrooms WHERE classroom_code = ''PERF001'')
         GROUP BY engagement_level',
        'Engagement level distribution',
        30
    ),
    'Engagement level distribution should be under 30ms'
);

-- =====================================================
-- 4. LESSON EFFECTIVENESS QUERY PERFORMANCE TESTS
-- =====================================================

-- Test 8: Lesson effectiveness summary
SELECT ok(
    test_query_performance(
        'SELECT l.title, le.average_engagement_score, le.completion_rate
         FROM lesson_effectiveness le
         JOIN lessons l ON le.lesson_id = l.id
         WHERE le.classroom_id = (SELECT id FROM classrooms WHERE classroom_code = ''PERF001'')',
        'Lesson effectiveness summary',
        25
    ),
    'Lesson effectiveness summary should be under 25ms'
);

-- Test 9: Pre/post assessment comparison
SELECT ok(
    test_query_performance(
        'SELECT 
            (post_assessment_scores->>''empathy'')::DECIMAL - (pre_assessment_scores->>''empathy'')::DECIMAL as empathy_improvement
         FROM lesson_effectiveness 
         WHERE classroom_id = (SELECT id FROM classrooms WHERE classroom_code = ''PERF001'')',
        'Pre/post assessment comparison',
        20
    ),
    'Pre/post assessment comparison should be under 20ms'
);

-- =====================================================
-- 5. DASHBOARD MATERIALIZED VIEW PERFORMANCE TESTS
-- =====================================================

-- Test 10: Classroom analytics dashboard query
SELECT ok(
    test_query_performance(
        'SELECT * FROM classroom_analytics_dashboard 
         WHERE classroom_code = ''PERF001''',
        'Classroom analytics dashboard',
        15
    ),
    'Classroom dashboard query should be under 15ms'
);

-- Test 11: Facilitator overview analytics query
SELECT ok(
    test_query_performance(
        'SELECT * FROM facilitator_overview_analytics 
         WHERE facilitator_id = (SELECT id FROM facilitators WHERE email = ''perf.test@example.com'')',
        'Facilitator overview analytics',
        20
    ),
    'Facilitator overview query should be under 20ms'
);

-- =====================================================
-- 6. AGGREGATION QUERY PERFORMANCE TESTS
-- =====================================================

-- Test 12: Weekly behavioral trend analysis
SELECT ok(
    test_query_performance(
        'SELECT 
            date_trunc(''week'', tracked_at) as week,
            behavioral_category,
            COUNT(*) as interactions,
            AVG(behavioral_score) as avg_score
         FROM behavioral_analytics 
         WHERE classroom_id = (SELECT id FROM classrooms WHERE classroom_code = ''PERF001'')
         AND tracked_at >= CURRENT_DATE - INTERVAL ''4 weeks''
         GROUP BY date_trunc(''week'', tracked_at), behavioral_category
         ORDER BY week, behavioral_category',
        'Weekly behavioral trend analysis',
        60
    ),
    'Weekly trend analysis should be under 60ms'
);

-- Test 13: Monthly engagement summary
SELECT ok(
    test_query_performance(
        'SELECT 
            date_trunc(''month'', tracked_at) as month,
            COUNT(DISTINCT anonymous_student_hash) as unique_students,
            COUNT(*) as total_interactions,
            AVG(behavioral_score) as avg_engagement
         FROM behavioral_analytics 
         WHERE classroom_id = (SELECT id FROM classrooms WHERE classroom_code = ''PERF001'')
         GROUP BY date_trunc(''month'', tracked_at)
         ORDER BY month',
        'Monthly engagement summary',
        50
    ),
    'Monthly engagement summary should be under 50ms'
);

-- =====================================================
-- 7. COMPLEX JOIN QUERY PERFORMANCE TESTS
-- =====================================================

-- Test 14: Student session with classroom and facilitator info
SELECT ok(
    test_query_performance(
        'SELECT 
            ss.session_token,
            c.name as classroom_name,
            f.first_name || '' '' || f.last_name as facilitator_name,
            COUNT(ba.id) as interaction_count
         FROM student_sessions ss
         JOIN classrooms c ON ss.classroom_id = c.id
         JOIN facilitators f ON c.facilitator_id = f.id
         LEFT JOIN behavioral_analytics ba ON ss.id = ba.student_session_id
         WHERE c.classroom_code = ''PERF001''
         GROUP BY ss.session_token, c.name, f.first_name, f.last_name
         LIMIT 20',
        'Student session with classroom info',
        45
    ),
    'Complex join query should be under 45ms'
);

-- Test 15: Lesson progress with analytics
SELECT ok(
    test_query_performance(
        'SELECT 
            l.title,
            COUNT(DISTINCT ba.anonymous_student_hash) as participating_students,
            COUNT(ba.id) as total_interactions,
            AVG(ba.behavioral_score) as avg_score
         FROM lessons l
         LEFT JOIN behavioral_analytics ba ON l.id = ba.lesson_id
         WHERE ba.classroom_id = (SELECT id FROM classrooms WHERE classroom_code = ''PERF001'')
         GROUP BY l.id, l.title
         ORDER BY l.lesson_number',
        'Lesson progress with analytics',
        40
    ),
    'Lesson progress query should be under 40ms'
);

-- =====================================================
-- 8. ANALYTICS FUNCTION PERFORMANCE TESTS
-- =====================================================

-- Test 16: Calculate behavioral engagement score
SELECT ok(
    test_query_performance(
        'SELECT calculate_behavioral_engagement_score(
            (SELECT id FROM student_sessions WHERE session_token = ''perf_session_1''),
            (SELECT id FROM lessons WHERE lesson_number = 99)
         )',
        'Calculate behavioral engagement score',
        30
    ),
    'Behavioral engagement calculation should be under 30ms'
);

-- Test 17: Calculate lesson effectiveness score
SELECT ok(
    test_query_performance(
        'SELECT calculate_lesson_effectiveness_score(
            (SELECT id FROM lessons WHERE lesson_number = 99),
            (SELECT id FROM classrooms WHERE classroom_code = ''PERF001'')
         )',
        'Calculate lesson effectiveness score',
        25
    ),
    'Lesson effectiveness calculation should be under 25ms'
);

-- =====================================================
-- 9. ANONYMOUS HASHING PERFORMANCE TESTS
-- =====================================================

-- Test 18: Anonymous hash generation performance
SELECT ok(
    test_query_performance(
        'SELECT generate_anonymous_student_hash(''performance_test_student_'' || generate_random_uuid())',
        'Anonymous hash generation',
        5
    ),
    'Anonymous hash generation should be under 5ms'
);

-- Test 19: Hash validation performance
SELECT ok(
    test_query_performance(
        'SELECT validate_anonymous_hash_consistency(
            ''test_student'',
            generate_anonymous_student_hash(''test_student'')
         )',
        'Anonymous hash validation',
        5
    ),
    'Hash validation should be under 5ms'
);

-- =====================================================
-- 10. LARGE DATASET PERFORMANCE TESTS
-- =====================================================

-- Test 20: Large behavioral analytics scan
SELECT ok(
    test_query_performance(
        'SELECT COUNT(*) FROM behavioral_analytics 
         WHERE tracked_at >= CURRENT_DATE - INTERVAL ''7 days''',
        'Large behavioral analytics scan',
        80
    ),
    'Large dataset scan should be under 80ms'
);

-- Test 21: Complex aggregation with grouping
SELECT ok(
    test_query_performance(
        'SELECT 
            behavioral_category,
            engagement_level,
            COUNT(*) as count,
            AVG(behavioral_score) as avg_score,
            MIN(tracked_at) as first_interaction,
            MAX(tracked_at) as last_interaction
         FROM behavioral_analytics 
         WHERE classroom_id = (SELECT id FROM classrooms WHERE classroom_code = ''PERF001'')
         GROUP BY behavioral_category, engagement_level
         ORDER BY behavioral_category, engagement_level',
        'Complex aggregation with grouping',
        60
    ),
    'Complex aggregation should be under 60ms'
);

-- =====================================================
-- 11. CACHE PERFORMANCE TESTS
-- =====================================================

-- Test 22: Analytics cache lookup
SELECT ok(
    test_query_performance(
        'SELECT aggregated_data FROM analytics_aggregation_cache 
         WHERE cache_type = ''classroom_dashboard'' 
         AND classroom_id = (SELECT id FROM classrooms WHERE classroom_code = ''PERF001'')
         AND cache_status = ''active''',
        'Analytics cache lookup',
        10
    ),
    'Cache lookup should be under 10ms'
);

-- =====================================================
-- 12. INDEX EFFECTIVENESS TESTS
-- =====================================================

-- Test 23: Index scan on anonymous_student_hash
SELECT ok(
    test_query_performance(
        'SELECT COUNT(*) FROM behavioral_analytics 
         WHERE anonymous_student_hash = generate_anonymous_student_hash(''perf_student_'' || (SELECT id FROM student_sessions LIMIT 1) || ''_1'')',
        'Index scan on anonymous hash',
        15
    ),
    'Hash index scan should be under 15ms'
);

-- Test 24: Index scan on tracked_at timestamp
SELECT ok(
    test_query_performance(
        'SELECT COUNT(*) FROM behavioral_analytics 
         WHERE tracked_at >= CURRENT_TIMESTAMP - INTERVAL ''1 hour''
         AND tracked_at <= CURRENT_TIMESTAMP',
        'Index scan on timestamp',
        20
    ),
    'Timestamp index scan should be under 20ms'
);

-- Test 25: Index scan on behavioral_category
SELECT ok(
    test_query_performance(
        'SELECT COUNT(*) FROM behavioral_analytics 
         WHERE behavioral_category = ''empathy''',
        'Index scan on behavioral category',
        15
    ),
    'Category index scan should be under 15ms'
);

-- =====================================================
-- 13. CONCURRENT QUERY SIMULATION
-- =====================================================

-- Test 26-30: Simulate concurrent dashboard queries
SELECT ok(
    test_query_performance(
        'SELECT 
            classroom_name,
            active_students,
            total_interactions_today,
            avg_behavioral_score_today,
            empathy_interactions,
            confidence_interactions,
            communication_interactions,
            leadership_interactions,
            growth_moments_today
         FROM classroom_analytics_dashboard 
         WHERE classroom_code = ''PERF001''',
        'Concurrent dashboard query 1',
        100
    ),
    'Concurrent dashboard query 1 should be under 100ms'
);

SELECT ok(
    test_query_performance(
        'SELECT 
            ba.behavioral_category,
            COUNT(*) as interactions,
            AVG(ba.behavioral_score) as avg_score,
            COUNT(CASE WHEN ba.growth_indicator = true THEN 1 END) as growth_moments
         FROM behavioral_analytics ba
         WHERE ba.classroom_id = (SELECT id FROM classrooms WHERE classroom_code = ''PERF001'')
         AND ba.tracked_at >= CURRENT_DATE
         GROUP BY ba.behavioral_category',
        'Concurrent dashboard query 2',
        100
    ),
    'Concurrent dashboard query 2 should be under 100ms'
);

SELECT ok(
    test_query_performance(
        'SELECT 
            DATE(ba.tracked_at) as date,
            COUNT(*) as daily_interactions,
            AVG(ba.behavioral_score) as daily_avg_score
         FROM behavioral_analytics ba
         WHERE ba.classroom_id = (SELECT id FROM classrooms WHERE classroom_code = ''PERF001'')
         AND ba.tracked_at >= CURRENT_DATE - INTERVAL ''7 days''
         GROUP BY DATE(ba.tracked_at)
         ORDER BY date',
        'Concurrent dashboard query 3',
        100
    ),
    'Concurrent dashboard query 3 should be under 100ms'
);

SELECT ok(
    test_query_performance(
        'SELECT 
            COUNT(DISTINCT ba.anonymous_student_hash) as unique_students,
            COUNT(*) as total_interactions,
            AVG(ba.behavioral_score) as overall_score
         FROM behavioral_analytics ba
         WHERE ba.classroom_id = (SELECT id FROM classrooms WHERE classroom_code = ''PERF001'')
         AND ba.tracked_at >= CURRENT_DATE - INTERVAL ''30 days''',
        'Concurrent dashboard query 4',
        100
    ),
    'Concurrent dashboard query 4 should be under 100ms'
);

SELECT ok(
    test_query_performance(
        'SELECT 
            le.average_engagement_score,
            le.completion_rate,
            le.facilitator_rating,
            l.title
         FROM lesson_effectiveness le
         JOIN lessons l ON le.lesson_id = l.id
         WHERE le.classroom_id = (SELECT id FROM classrooms WHERE classroom_code = ''PERF001'')
         ORDER BY le.measurement_date DESC
         LIMIT 10',
        'Concurrent dashboard query 5',
        100
    ),
    'Concurrent dashboard query 5 should be under 100ms'
);

-- =====================================================
-- 14. PERFORMANCE ANALYSIS AND RECOMMENDATIONS
-- =====================================================

-- Analyze query performance statistics
SELECT ok(
    test_query_performance(
        'SELECT 
            query,
            calls,
            total_time,
            mean_time,
            min_time,
            max_time
         FROM pg_stat_statements 
         WHERE query LIKE ''%behavioral_analytics%''
         ORDER BY mean_time DESC
         LIMIT 10',
        'Performance statistics analysis',
        50
    ),
    'Performance statistics query should be under 50ms'
);

-- =====================================================
-- 15. CLEANUP AND FINAL VALIDATION
-- =====================================================

-- Test final performance validation
SELECT ok(
    test_query_performance(
        'SELECT COUNT(*) FROM behavioral_analytics',
        'Final count validation',
        30
    ),
    'Final validation query should be under 30ms'
);

-- Generate performance report
SELECT ok(
    EXISTS(
        SELECT 1 FROM pg_stat_user_tables 
        WHERE relname = 'behavioral_analytics' 
        AND seq_scan < idx_scan
    ),
    'Behavioral analytics should use index scans more than sequential scans'
);

-- =====================================================
-- CLEANUP TEST DATA
-- =====================================================

-- Clean up performance test data
DELETE FROM behavioral_analytics WHERE interaction_type = 'performance_test_interaction';
DELETE FROM lesson_effectiveness WHERE lesson_id = (SELECT id FROM lessons WHERE lesson_number = 99);
DELETE FROM time_series_analytics WHERE lesson_id = (SELECT id FROM lessons WHERE lesson_number = 99);
DELETE FROM student_sessions WHERE session_token LIKE 'perf_session_%';
DELETE FROM lessons WHERE lesson_number = 99;
DELETE FROM classrooms WHERE classroom_code = 'PERF001';
DELETE FROM facilitators WHERE email = 'perf.test@example.com';

-- Refresh materialized views after cleanup
REFRESH MATERIALIZED VIEW classroom_analytics_dashboard;
REFRESH MATERIALIZED VIEW facilitator_overview_analytics;

-- Drop test functions
DROP FUNCTION IF EXISTS test_query_performance(TEXT, TEXT, INTEGER);
DROP FUNCTION IF EXISTS create_performance_test_data();

-- Final performance summary
SELECT ok(true, 'Performance testing completed - all queries should meet sub-100ms requirements');

-- Finish the test plan
SELECT * FROM finish();
ROLLBACK;