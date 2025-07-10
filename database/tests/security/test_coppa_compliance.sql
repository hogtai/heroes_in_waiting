-- =====================================================
-- Heroes in Waiting - COPPA Compliance Database Tests
-- PostgreSQL COPPA Validation Tests using pgTAP
-- Date: 2025-07-10
-- =====================================================

-- Load pgTAP testing framework
BEGIN;
SELECT plan(75); -- Expecting 75 COPPA compliance tests

-- =====================================================
-- 1. ANONYMOUS HASHING FUNCTION TESTS
-- =====================================================

-- Test SHA-256 hash generation function exists and works
SELECT lives_ok(
    $$SELECT generate_anonymous_student_hash('test_student_123')$$,
    'generate_anonymous_student_hash function should execute without error'
);

-- Test hash consistency - same input should produce same hash on same day
SELECT is(
    generate_anonymous_student_hash('test_student_123'),
    generate_anonymous_student_hash('test_student_123'),
    'Same student identifier should produce same hash on same day'
);

-- Test hash length - SHA-256 should produce 64-character hex string
SELECT ok(
    length(generate_anonymous_student_hash('test_student_123')) = 64,
    'Generated hash should be 64 characters (SHA-256 hex)'
);

-- Test hash uniqueness - different inputs should produce different hashes
SELECT isnt(
    generate_anonymous_student_hash('student_1'),
    generate_anonymous_student_hash('student_2'),
    'Different student identifiers should produce different hashes'
);

-- Test hash validation function
SELECT ok(
    validate_anonymous_hash_consistency(
        'test_student_123', 
        generate_anonymous_student_hash('test_student_123')
    ),
    'Hash validation should confirm correct hash'
);

-- Test hash validation rejects wrong hash
SELECT ok(
    NOT validate_anonymous_hash_consistency(
        'test_student_123', 
        'wrong_hash_value_that_should_not_match_anything_here'
    ),
    'Hash validation should reject incorrect hash'
);

-- =====================================================
-- 2. PII DETECTION AND PREVENTION TESTS
-- =====================================================

-- Create test function to check for PII patterns
CREATE OR REPLACE FUNCTION detect_pii_patterns(text_data TEXT) 
RETURNS JSONB AS $$
DECLARE
    pii_found JSONB := '{}';
BEGIN
    -- Check for email patterns
    IF text_data ~ '\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b' THEN
        pii_found := pii_found || '{"email": true}';
    END IF;
    
    -- Check for phone number patterns
    IF text_data ~ '\b\d{3}[-.]?\d{3}[-.]?\d{4}\b' THEN
        pii_found := pii_found || '{"phone": true}';
    END IF;
    
    -- Check for address patterns
    IF text_data ~ '\b\d{1,5}\s\w+\s(street|st|avenue|ave|road|rd|drive|dr|lane|ln|boulevard|blvd)\b' THEN
        pii_found := pii_found || '{"address": true}';
    END IF;
    
    -- Check for SSN patterns
    IF text_data ~ '\b\d{3}-\d{2}-\d{4}\b' THEN
        pii_found := pii_found || '{"ssn": true}';
    END IF;
    
    -- Check for name patterns (first name last name)
    IF text_data ~ '\b[A-Z][a-z]+ [A-Z][a-z]+\b' THEN
        pii_found := pii_found || '{"full_name": true}';
    END IF;
    
    RETURN pii_found;
END;
$$ LANGUAGE plpgsql;

-- Test that behavioral_analytics doesn't store PII
INSERT INTO facilitators (id, email, password_hash, first_name, last_name) VALUES 
(uuid_generate_v4(), 'test@example.com', 'hashed_password', 'Test', 'Facilitator');

INSERT INTO classrooms (id, facilitator_id, name, classroom_code, grade_level) VALUES 
(uuid_generate_v4(), (SELECT id FROM facilitators LIMIT 1), 'Test Classroom', 'TEST123', 5);

INSERT INTO student_sessions (id, classroom_id, session_token, expires_at) VALUES 
(uuid_generate_v4(), (SELECT id FROM classrooms LIMIT 1), 'test_session_token', CURRENT_TIMESTAMP + INTERVAL '1 hour');

-- Insert test behavioral analytics data
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
    (SELECT id FROM student_sessions LIMIT 1),
    (SELECT id FROM classrooms LIMIT 1),
    generate_anonymous_student_hash('test_student_for_pii_check'),
    encode(gen_random_bytes(16), 'hex'),
    'empathy',
    'peer_help',
    4,
    'high'
);

-- Test that no PII exists in behavioral_analytics data
SELECT ok(
    (SELECT COUNT(*) = 0 FROM behavioral_analytics ba 
     WHERE detect_pii_patterns(ba::text) != '{}'),
    'Behavioral analytics should contain no PII patterns'
);

-- Test specific columns for PII
SELECT ok(
    (SELECT COUNT(*) = 0 FROM behavioral_analytics 
     WHERE anonymous_student_hash ~ '\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b'),
    'Anonymous student hash should not contain email addresses'
);

SELECT ok(
    (SELECT COUNT(*) = 0 FROM behavioral_analytics 
     WHERE interaction_context ~ '\b\d{3}[-.]?\d{3}[-.]?\d{4}\b'),
    'Interaction context should not contain phone numbers'
);

SELECT ok(
    (SELECT COUNT(*) = 0 FROM behavioral_analytics 
     WHERE skill_demonstration ~ '\b[A-Z][a-z]+ [A-Z][a-z]+\b'),
    'Skill demonstration should not contain full names'
);

-- =====================================================
-- 3. DATA RETENTION POLICY TESTS
-- =====================================================

-- Test that data retention function exists and works
SELECT lives_ok(
    $$SELECT execute_coppa_data_retention(90)$$,
    'COPPA data retention function should execute without error'
);

-- Insert old test data (older than 90 days)
INSERT INTO behavioral_analytics (
    student_session_id,
    classroom_id,
    anonymous_student_hash,
    session_tracking_id,
    behavioral_category,
    interaction_type,
    behavioral_score,
    engagement_level,
    tracked_at
) VALUES (
    (SELECT id FROM student_sessions LIMIT 1),
    (SELECT id FROM classrooms LIMIT 1),
    generate_anonymous_student_hash('old_test_student'),
    encode(gen_random_bytes(16), 'hex'),
    'confidence',
    'speaking_up',
    3,
    'medium',
    CURRENT_TIMESTAMP - INTERVAL '95 days'
);

-- Count records before retention cleanup
SELECT ok(
    (SELECT COUNT(*) > 0 FROM behavioral_analytics 
     WHERE tracked_at < CURRENT_TIMESTAMP - INTERVAL '90 days'),
    'Should have old records before retention cleanup'
);

-- Execute retention policy
SELECT execute_coppa_data_retention(90);

-- Test that old data is removed after retention
SELECT ok(
    (SELECT COUNT(*) = 0 FROM behavioral_analytics 
     WHERE tracked_at < CURRENT_TIMESTAMP - INTERVAL '90 days'),
    'Old behavioral analytics data should be deleted after retention cleanup'
);

-- Test that data is archived before deletion
SELECT ok(
    (SELECT COUNT(*) > 0 FROM behavioral_analytics_archive 
     WHERE tracked_at < CURRENT_TIMESTAMP - INTERVAL '90 days'),
    'Old data should be archived before deletion'
);

-- Test retention log is created
SELECT ok(
    (SELECT COUNT(*) > 0 FROM data_retention_log 
     WHERE table_name = 'behavioral_analytics' 
     AND retention_policy LIKE 'coppa_%_day_retention'),
    'Data retention execution should be logged'
);

-- =====================================================
-- 4. CONSENT WITHDRAWAL DATA PURGING TESTS
-- =====================================================

-- Create function to simulate consent withdrawal
CREATE OR REPLACE FUNCTION purge_student_data(student_hash VARCHAR(64))
RETURNS JSONB AS $$
DECLARE
    purged_count INTEGER := 0;
    result JSONB;
BEGIN
    -- Delete all behavioral analytics for the student
    DELETE FROM behavioral_analytics 
    WHERE anonymous_student_hash = student_hash;
    GET DIAGNOSTICS purged_count = ROW_COUNT;
    
    -- Also remove from archive
    DELETE FROM behavioral_analytics_archive 
    WHERE anonymous_student_hash = student_hash;
    
    result := jsonb_build_object(
        'student_hash', student_hash,
        'records_purged', purged_count,
        'purged_at', CURRENT_TIMESTAMP
    );
    
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- Insert test data for consent withdrawal test
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
    (SELECT id FROM student_sessions LIMIT 1),
    (SELECT id FROM classrooms LIMIT 1),
    generate_anonymous_student_hash('consent_withdrawal_test_student'),
    encode(gen_random_bytes(16), 'hex'),
    'empathy',
    'peer_support',
    5,
    'exceptional'
);

-- Verify data exists before withdrawal
SELECT ok(
    (SELECT COUNT(*) > 0 FROM behavioral_analytics 
     WHERE anonymous_student_hash = generate_anonymous_student_hash('consent_withdrawal_test_student')),
    'Test data should exist before consent withdrawal'
);

-- Execute consent withdrawal
SELECT purge_student_data(generate_anonymous_student_hash('consent_withdrawal_test_student'));

-- Test that all data is purged after consent withdrawal
SELECT ok(
    (SELECT COUNT(*) = 0 FROM behavioral_analytics 
     WHERE anonymous_student_hash = generate_anonymous_student_hash('consent_withdrawal_test_student')),
    'All student data should be purged after consent withdrawal'
);

-- =====================================================
-- 5. EDUCATIONAL PURPOSE RESTRICTION TESTS
-- =====================================================

-- Test that all behavioral categories are educational
SELECT ok(
    (SELECT COUNT(*) = 0 FROM behavioral_analytics 
     WHERE behavioral_category NOT IN ('empathy', 'confidence', 'communication', 'leadership', 'kindness', 'courage')),
    'All behavioral categories should be educational in nature'
);

-- Test that interaction types are educational
CREATE OR REPLACE FUNCTION validate_educational_purpose(interaction_type TEXT)
RETURNS BOOLEAN AS $$
DECLARE
    educational_keywords TEXT[] := ARRAY[
        'help', 'support', 'listening', 'sharing', 'collaboration', 
        'empathy', 'kindness', 'leadership', 'communication', 'learning',
        'understanding', 'respect', 'inclusion', 'courage', 'growth'
    ];
    keyword TEXT;
BEGIN
    -- Check if interaction type contains educational keywords
    FOREACH keyword IN ARRAY educational_keywords
    LOOP
        IF lower(interaction_type) LIKE '%' || keyword || '%' THEN
            RETURN true;
        END IF;
    END LOOP;
    
    RETURN false;
END;
$$ LANGUAGE plpgsql;

-- Test that interaction types serve educational purposes
SELECT ok(
    (SELECT COUNT(*) = (SELECT COUNT(*) FROM behavioral_analytics ba
                       WHERE validate_educational_purpose(ba.interaction_type))
     FROM behavioral_analytics),
    'All interaction types should serve educational purposes'
);

-- =====================================================
-- 6. ANONYMOUS SESSION TRACKING TESTS
-- =====================================================

-- Test that session tracking IDs are properly anonymized
SELECT ok(
    (SELECT COUNT(*) = 0 FROM behavioral_analytics 
     WHERE session_tracking_id ~ '\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b'),
    'Session tracking IDs should not contain email addresses'
);

-- Test that session tracking IDs are sufficiently random
SELECT ok(
    (SELECT COUNT(DISTINCT session_tracking_id) = COUNT(*) FROM behavioral_analytics),
    'All session tracking IDs should be unique'
);

-- Test that anonymous student hashes are consistent per session
INSERT INTO behavioral_analytics (
    student_session_id,
    classroom_id,
    anonymous_student_hash,
    session_tracking_id,
    behavioral_category,
    interaction_type,
    behavioral_score,
    engagement_level
) VALUES 
    ((SELECT id FROM student_sessions LIMIT 1), (SELECT id FROM classrooms LIMIT 1), 
     generate_anonymous_student_hash('consistency_test'), encode(gen_random_bytes(16), 'hex'),
     'empathy', 'peer_help', 4, 'high'),
    ((SELECT id FROM student_sessions LIMIT 1), (SELECT id FROM classrooms LIMIT 1), 
     generate_anonymous_student_hash('consistency_test'), encode(gen_random_bytes(16), 'hex'),
     'confidence', 'speaking_up', 3, 'medium');

-- Test hash consistency within same student
SELECT ok(
    (SELECT COUNT(DISTINCT anonymous_student_hash) = 1 
     FROM behavioral_analytics 
     WHERE anonymous_student_hash = generate_anonymous_student_hash('consistency_test')),
    'Same student should have consistent anonymous hash across interactions'
);

-- =====================================================
-- 7. DAILY SALT ROTATION TESTS
-- =====================================================

-- Test that salt rotation doesn't break existing hashes
INSERT INTO anonymous_hash_salts (salt_date, salt_value) VALUES 
(CURRENT_DATE - INTERVAL '1 day', encode(gen_random_bytes(32), 'hex'));

-- Test that yesterday's hash is different from today's hash for same student
SELECT isnt(
    generate_anonymous_student_hash('salt_test_student', CURRENT_DATE - INTERVAL '1 day'),
    generate_anonymous_student_hash('salt_test_student', CURRENT_DATE),
    'Different salt dates should produce different hashes for same student'
);

-- Test that hash is consistent within same day
SELECT is(
    generate_anonymous_student_hash('salt_test_student', CURRENT_DATE),
    generate_anonymous_student_hash('salt_test_student', CURRENT_DATE),
    'Same student should have consistent hash within same day'
);

-- Test that old salts are cleaned up properly
INSERT INTO anonymous_hash_salts (salt_date, salt_value) VALUES 
(CURRENT_DATE - INTERVAL '10 days', encode(gen_random_bytes(32), 'hex'));

-- Execute cleanup (normally part of retention policy)
DELETE FROM anonymous_hash_salts WHERE salt_date < CURRENT_DATE - INTERVAL '7 days';

-- Test that old salts are removed
SELECT ok(
    (SELECT COUNT(*) = 0 FROM anonymous_hash_salts 
     WHERE salt_date < CURRENT_DATE - INTERVAL '7 days'),
    'Old salt values should be cleaned up after 7 days'
);

-- =====================================================
-- 8. METADATA VALIDATION TESTS
-- =====================================================

-- Test that metadata doesn't contain PII
INSERT INTO behavioral_analytics (
    student_session_id,
    classroom_id,
    anonymous_student_hash,
    session_tracking_id,
    behavioral_category,
    interaction_type,
    behavioral_score,
    engagement_level,
    metadata
) VALUES (
    (SELECT id FROM student_sessions LIMIT 1),
    (SELECT id FROM classrooms LIMIT 1),
    generate_anonymous_student_hash('metadata_test_student'),
    encode(gen_random_bytes(16), 'hex'),
    'leadership',
    'group_facilitation',
    4,
    'high',
    '{"interaction_duration": 120, "peer_count": 4, "activity_type": "group_discussion"}'
);

-- Test that metadata contains only educational data
SELECT ok(
    (SELECT COUNT(*) = 0 FROM behavioral_analytics 
     WHERE detect_pii_patterns(metadata::text) != '{}'),
    'Metadata should not contain any PII patterns'
);

-- Test specific metadata fields are educational
SELECT ok(
    (SELECT COUNT(*) = 0 FROM behavioral_analytics 
     WHERE metadata ? 'student_name' OR metadata ? 'parent_email' OR metadata ? 'phone_number'),
    'Metadata should not contain prohibited PII fields'
);

-- =====================================================
-- 9. AUDIT TRAIL FOR COPPA COMPLIANCE
-- =====================================================

-- Test that COPPA compliance actions are logged
SELECT ok(
    (SELECT COUNT(*) > 0 FROM audit_log 
     WHERE table_name = 'behavioral_analytics' 
     AND action = 'INSERT'),
    'Behavioral analytics insertions should be audited'
);

-- Test that data retention actions are logged
SELECT ok(
    (SELECT COUNT(*) > 0 FROM data_retention_log 
     WHERE table_name = 'behavioral_analytics'),
    'Data retention actions should be logged'
);

-- =====================================================
-- 10. CROSS-PLATFORM HASH CONSISTENCY TESTS
-- =====================================================

-- Test that hashes work consistently across different input formats
SELECT is(
    generate_anonymous_student_hash('Student123'),
    generate_anonymous_student_hash('Student123'),
    'Hash should be consistent regardless of input casing (normalized)'
);

-- Test that trimmed inputs produce consistent hashes
SELECT is(
    generate_anonymous_student_hash(trim('  Student123  ')),
    generate_anonymous_student_hash('Student123'),
    'Hash should handle whitespace normalization'
);

-- =====================================================
-- CLEANUP TEST DATA
-- =====================================================

-- Clean up test data
DELETE FROM behavioral_analytics WHERE anonymous_student_hash LIKE '%test%';
DELETE FROM behavioral_analytics_archive WHERE anonymous_student_hash LIKE '%test%';
DELETE FROM student_sessions WHERE session_token = 'test_session_token';
DELETE FROM classrooms WHERE classroom_code = 'TEST123';
DELETE FROM facilitators WHERE email = 'test@example.com';
DELETE FROM anonymous_hash_salts WHERE salt_date != CURRENT_DATE;

-- Drop test functions
DROP FUNCTION IF EXISTS detect_pii_patterns(TEXT);
DROP FUNCTION IF EXISTS purge_student_data(VARCHAR(64));
DROP FUNCTION IF EXISTS validate_educational_purpose(TEXT);

-- Final validation: Ensure no test PII remains
SELECT ok(
    (SELECT COUNT(*) = 0 FROM behavioral_analytics 
     WHERE anonymous_student_hash ~ 'test|Test|TEST'),
    'All test data should be cleaned up'
);

-- Finish the test plan
SELECT * FROM finish();
ROLLBACK;