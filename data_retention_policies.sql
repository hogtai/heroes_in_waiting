-- =====================================================
-- Heroes in Waiting - Data Retention Policies
-- 2-Year Active Data Retention, 3-Year Backup Retention
-- Weekly Backup Schedule Implementation
-- =====================================================

-- =====================================================
-- 1. DATA RETENTION POLICY CONFIGURATION
-- =====================================================

-- Create retention policy configuration table
CREATE TABLE IF NOT EXISTS retention_policies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    table_name VARCHAR(100) NOT NULL,
    policy_name VARCHAR(100) NOT NULL,
    retention_period_days INTEGER NOT NULL,
    archive_period_days INTEGER,
    policy_type VARCHAR(20) DEFAULT 'time_based' CHECK (policy_type IN ('time_based', 'count_based', 'custom')),
    is_active BOOLEAN DEFAULT true,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert retention policies for each table
INSERT INTO retention_policies (table_name, policy_name, retention_period_days, archive_period_days, description) VALUES
('audit_log', 'audit_2_year_retention', 730, 1095, 'Audit logs: 2 years active, 3 years total retention'),
('student_sessions', 'student_session_2_year_retention', 730, 1095, 'Student sessions: 2 years active, 3 years total retention'),
('student_progress', 'progress_2_year_retention', 730, 1095, 'Student progress: 2 years active, 3 years total retention'),
('student_feedback', 'feedback_2_year_retention', 730, 1095, 'Student feedback: 2 years active, 3 years total retention'),
('classroom_sessions', 'classroom_session_2_year_retention', 730, 1095, 'Classroom sessions: 2 years active, 3 years total retention'),
('facilitator_sessions', 'facilitator_session_30_day_retention', 30, NULL, 'Facilitator sessions: 30 days active retention'),
('security_incidents', 'security_incident_2_year_retention', 730, 1095, 'Security incidents: 2 years active, 3 years total retention'),
('content_cache', 'cache_90_day_retention', 90, NULL, 'Content cache: 90 days retention'),
('offline_sync_queue', 'sync_queue_7_day_retention', 7, NULL, 'Offline sync queue: 7 days retention'),
('job_queue', 'job_queue_30_day_retention', 30, NULL, 'Job queue: 30 days retention');

-- =====================================================
-- 2. ARCHIVE TABLES FOR LONG-TERM STORAGE
-- =====================================================

-- Create archive tables with same structure as main tables
CREATE TABLE student_sessions_archive (LIKE student_sessions INCLUDING ALL);
CREATE TABLE student_progress_archive (LIKE student_progress INCLUDING ALL);
CREATE TABLE student_feedback_archive (LIKE student_feedback INCLUDING ALL);
CREATE TABLE classroom_sessions_archive (LIKE classroom_sessions INCLUDING ALL);
CREATE TABLE security_incidents_archive (LIKE security_incidents INCLUDING ALL);

-- Add archival metadata to archive tables
ALTER TABLE student_sessions_archive ADD COLUMN archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE student_progress_archive ADD COLUMN archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE student_feedback_archive ADD COLUMN archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE classroom_sessions_archive ADD COLUMN archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE security_incidents_archive ADD COLUMN archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE audit_log_archive ADD COLUMN archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- =====================================================
-- 3. DATA RETENTION EXECUTION FUNCTIONS
-- =====================================================

-- Enhanced data retention function with archiving
CREATE OR REPLACE FUNCTION execute_data_retention_policy(
    target_table VARCHAR(100) DEFAULT NULL
) RETURNS TABLE (
    table_name VARCHAR(100),
    records_archived INTEGER,
    records_deleted INTEGER,
    execution_time_seconds INTEGER
) AS $$
DECLARE
    policy_record RECORD;
    start_time TIMESTAMP;
    archive_count INTEGER;
    delete_count INTEGER;
    exec_time INTEGER;
    retention_date DATE;
    archive_date DATE;
    sql_query TEXT;
BEGIN
    start_time := CURRENT_TIMESTAMP;
    
    -- Loop through all active retention policies
    FOR policy_record IN 
        SELECT * FROM retention_policies 
        WHERE is_active = true 
        AND (target_table IS NULL OR retention_policies.table_name = target_table)
    LOOP
        archive_count := 0;
        delete_count := 0;
        
        -- Calculate retention dates
        retention_date := CURRENT_DATE - INTERVAL '1 day' * policy_record.retention_period_days;
        
        IF policy_record.archive_period_days IS NOT NULL THEN
            archive_date := CURRENT_DATE - INTERVAL '1 day' * policy_record.archive_period_days;
        END IF;
        
        -- Execute retention policy based on table
        CASE policy_record.table_name
            WHEN 'audit_log' THEN
                -- Archive audit logs older than 2 years
                INSERT INTO audit_log_archive 
                SELECT *, CURRENT_TIMESTAMP FROM audit_log 
                WHERE timestamp::DATE < retention_date;
                GET DIAGNOSTICS archive_count = ROW_COUNT;
                
                -- Delete archived records from main table
                DELETE FROM audit_log WHERE timestamp::DATE < retention_date;
                GET DIAGNOSTICS delete_count = ROW_COUNT;
                
                -- Delete archived records older than 3 years
                IF archive_date IS NOT NULL THEN
                    DELETE FROM audit_log_archive WHERE timestamp::DATE < archive_date;
                END IF;
                
            WHEN 'student_sessions' THEN
                -- Archive student sessions older than 2 years
                INSERT INTO student_sessions_archive 
                SELECT *, CURRENT_TIMESTAMP FROM student_sessions 
                WHERE created_at::DATE < retention_date;
                GET DIAGNOSTICS archive_count = ROW_COUNT;
                
                DELETE FROM student_sessions WHERE created_at::DATE < retention_date;
                GET DIAGNOSTICS delete_count = ROW_COUNT;
                
                -- Delete archived records older than 3 years
                IF archive_date IS NOT NULL THEN
                    DELETE FROM student_sessions_archive WHERE created_at::DATE < archive_date;
                END IF;
                
            WHEN 'student_progress' THEN
                -- Archive student progress older than 2 years
                INSERT INTO student_progress_archive 
                SELECT *, CURRENT_TIMESTAMP FROM student_progress 
                WHERE created_at::DATE < retention_date;
                GET DIAGNOSTICS archive_count = ROW_COUNT;
                
                DELETE FROM student_progress WHERE created_at::DATE < retention_date;
                GET DIAGNOSTICS delete_count = ROW_COUNT;
                
                IF archive_date IS NOT NULL THEN
                    DELETE FROM student_progress_archive WHERE created_at::DATE < archive_date;
                END IF;
                
            WHEN 'student_feedback' THEN
                -- Archive student feedback older than 2 years
                INSERT INTO student_feedback_archive 
                SELECT *, CURRENT_TIMESTAMP FROM student_feedback 
                WHERE submitted_at::DATE < retention_date;
                GET DIAGNOSTICS archive_count = ROW_COUNT;
                
                DELETE FROM student_feedback WHERE submitted_at::DATE < retention_date;
                GET DIAGNOSTICS delete_count = ROW_COUNT;
                
                IF archive_date IS NOT NULL THEN
                    DELETE FROM student_feedback_archive WHERE submitted_at::DATE < archive_date;
                END IF;
                
            WHEN 'classroom_sessions' THEN
                -- Archive classroom sessions older than 2 years
                INSERT INTO classroom_sessions_archive 
                SELECT *, CURRENT_TIMESTAMP FROM classroom_sessions 
                WHERE created_at::DATE < retention_date;
                GET DIAGNOSTICS archive_count = ROW_COUNT;
                
                DELETE FROM classroom_sessions WHERE created_at::DATE < retention_date;
                GET DIAGNOSTICS delete_count = ROW_COUNT;
                
                IF archive_date IS NOT NULL THEN
                    DELETE FROM classroom_sessions_archive WHERE created_at::DATE < archive_date;
                END IF;
                
            WHEN 'facilitator_sessions' THEN
                -- Delete expired facilitator sessions (no archiving needed)
                DELETE FROM facilitator_sessions 
                WHERE created_at::DATE < retention_date OR expires_at < CURRENT_TIMESTAMP;
                GET DIAGNOSTICS delete_count = ROW_COUNT;
                
            WHEN 'security_incidents' THEN
                -- Archive security incidents older than 2 years
                INSERT INTO security_incidents_archive 
                SELECT *, CURRENT_TIMESTAMP FROM security_incidents 
                WHERE created_at::DATE < retention_date;
                GET DIAGNOSTICS archive_count = ROW_COUNT;
                
                DELETE FROM security_incidents WHERE created_at::DATE < retention_date;
                GET DIAGNOSTICS delete_count = ROW_COUNT;
                
                IF archive_date IS NOT NULL THEN
                    DELETE FROM security_incidents_archive WHERE created_at::DATE < archive_date;
                END IF;
                
            WHEN 'content_cache' THEN
                -- Delete old cache entries (no archiving needed)
                DELETE FROM content_cache 
                WHERE last_accessed_at::DATE < retention_date OR expires_at < CURRENT_TIMESTAMP;
                GET DIAGNOSTICS delete_count = ROW_COUNT;
                
            WHEN 'offline_sync_queue' THEN
                -- Delete old sync queue entries (no archiving needed)
                DELETE FROM offline_sync_queue 
                WHERE created_at::DATE < retention_date AND status IN ('completed', 'failed');
                GET DIAGNOSTICS delete_count = ROW_COUNT;
                
            WHEN 'job_queue' THEN
                -- Delete old job queue entries (no archiving needed)
                DELETE FROM job_queue 
                WHERE created_at::DATE < retention_date AND status IN ('completed', 'failed', 'cancelled');
                GET DIAGNOSTICS delete_count = ROW_COUNT;
                
            ELSE
                -- Log unknown table
                RAISE NOTICE 'Unknown table for retention policy: %', policy_record.table_name;
        END CASE;
        
        -- Calculate execution time
        exec_time := EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - start_time));
        
        -- Log retention execution
        INSERT INTO data_retention_log (
            table_name, retention_policy, records_processed, 
            records_archived, records_deleted, execution_date, 
            execution_time_seconds, status
        ) VALUES (
            policy_record.table_name, policy_record.policy_name, 
            archive_count + delete_count, archive_count, delete_count, 
            CURRENT_DATE, exec_time, 'completed'
        );
        
        -- Return results
        table_name := policy_record.table_name;
        records_archived := archive_count;
        records_deleted := delete_count;
        execution_time_seconds := exec_time;
        RETURN NEXT;
        
    END LOOP;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;

-- Function to get retention policy status
CREATE OR REPLACE FUNCTION get_retention_status()
RETURNS TABLE (
    table_name VARCHAR(100),
    total_records BIGINT,
    old_records BIGINT,
    retention_date DATE,
    needs_cleanup BOOLEAN
) AS $$
DECLARE
    policy_record RECORD;
    record_count BIGINT;
    old_count BIGINT;
    retention_cutoff DATE;
BEGIN
    FOR policy_record IN SELECT * FROM retention_policies WHERE is_active = true
    LOOP
        retention_cutoff := CURRENT_DATE - INTERVAL '1 day' * policy_record.retention_period_days;
        
        -- Get record counts based on table
        CASE policy_record.table_name
            WHEN 'audit_log' THEN
                SELECT COUNT(*) INTO record_count FROM audit_log;
                SELECT COUNT(*) INTO old_count FROM audit_log WHERE timestamp::DATE < retention_cutoff;
            WHEN 'student_sessions' THEN
                SELECT COUNT(*) INTO record_count FROM student_sessions;
                SELECT COUNT(*) INTO old_count FROM student_sessions WHERE created_at::DATE < retention_cutoff;
            WHEN 'student_progress' THEN
                SELECT COUNT(*) INTO record_count FROM student_progress;
                SELECT COUNT(*) INTO old_count FROM student_progress WHERE created_at::DATE < retention_cutoff;
            WHEN 'student_feedback' THEN
                SELECT COUNT(*) INTO record_count FROM student_feedback;
                SELECT COUNT(*) INTO old_count FROM student_feedback WHERE submitted_at::DATE < retention_cutoff;
            WHEN 'classroom_sessions' THEN
                SELECT COUNT(*) INTO record_count FROM classroom_sessions;
                SELECT COUNT(*) INTO old_count FROM classroom_sessions WHERE created_at::DATE < retention_cutoff;
            WHEN 'facilitator_sessions' THEN
                SELECT COUNT(*) INTO record_count FROM facilitator_sessions;
                SELECT COUNT(*) INTO old_count FROM facilitator_sessions WHERE created_at::DATE < retention_cutoff;
            WHEN 'security_incidents' THEN
                SELECT COUNT(*) INTO record_count FROM security_incidents;
                SELECT COUNT(*) INTO old_count FROM security_incidents WHERE created_at::DATE < retention_cutoff;
            WHEN 'content_cache' THEN
                SELECT COUNT(*) INTO record_count FROM content_cache;
                SELECT COUNT(*) INTO old_count FROM content_cache WHERE last_accessed_at::DATE < retention_cutoff;
            WHEN 'offline_sync_queue' THEN
                SELECT COUNT(*) INTO record_count FROM offline_sync_queue;
                SELECT COUNT(*) INTO old_count FROM offline_sync_queue WHERE created_at::DATE < retention_cutoff;
            WHEN 'job_queue' THEN
                SELECT COUNT(*) INTO record_count FROM job_queue;
                SELECT COUNT(*) INTO old_count FROM job_queue WHERE created_at::DATE < retention_cutoff;
            ELSE
                record_count := 0;
                old_count := 0;
        END CASE;
        
        -- Return results
        table_name := policy_record.table_name;
        total_records := record_count;
        old_records := old_count;
        retention_date := retention_cutoff;
        needs_cleanup := old_count > 0;
        RETURN NEXT;
    END LOOP;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 4. AUTOMATED RETENTION POLICY EXECUTION
-- =====================================================

-- Schedule retention policy execution (to be called by cron job)
CREATE OR REPLACE FUNCTION schedule_retention_cleanup() RETURNS VOID AS $$
BEGIN
    -- Insert job to execute retention policies
    INSERT INTO job_queue (job_type, priority, payload, scheduled_at) VALUES
    ('data_retention_cleanup', 1, '{"action": "execute_all_policies"}', CURRENT_TIMESTAMP);
    
    -- Log the scheduling
    INSERT INTO audit_log (table_name, action, new_values, timestamp) VALUES
    ('retention_policies', 'SCHEDULE', '{"action": "retention_cleanup_scheduled"}', CURRENT_TIMESTAMP);
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 5. MONITORING AND REPORTING
-- =====================================================

-- Create view for retention policy monitoring
CREATE VIEW retention_policy_status AS
SELECT 
    rp.table_name,
    rp.policy_name,
    rp.retention_period_days,
    rp.archive_period_days,
    rp.is_active,
    drl.execution_date AS last_execution,
    drl.records_archived AS last_archived_count,
    drl.records_deleted AS last_deleted_count,
    drl.execution_time_seconds AS last_execution_time
FROM retention_policies rp
LEFT JOIN LATERAL (
    SELECT * FROM data_retention_log 
    WHERE table_name = rp.table_name 
    ORDER BY execution_date DESC 
    LIMIT 1
) drl ON true
WHERE rp.is_active = true;

-- Function to generate retention report
CREATE OR REPLACE FUNCTION generate_retention_report(
    start_date DATE DEFAULT CURRENT_DATE - INTERVAL '30 days',
    end_date DATE DEFAULT CURRENT_DATE
) RETURNS TABLE (
    report_date DATE,
    total_tables INTEGER,
    total_archived BIGINT,
    total_deleted BIGINT,
    total_execution_time INTEGER,
    largest_cleanup VARCHAR(100),
    cleanup_summary JSONB
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        drl.execution_date,
        COUNT(DISTINCT drl.table_name)::INTEGER,
        SUM(drl.records_archived),
        SUM(drl.records_deleted),
        SUM(drl.execution_time_seconds)::INTEGER,
        (SELECT table_name FROM data_retention_log drl2 
         WHERE drl2.execution_date = drl.execution_date 
         ORDER BY (records_archived + records_deleted) DESC LIMIT 1),
        json_agg(
            json_build_object(
                'table', drl.table_name,
                'archived', drl.records_archived,
                'deleted', drl.records_deleted,
                'time', drl.execution_time_seconds
            )
        )::JSONB
    FROM data_retention_log drl
    WHERE drl.execution_date BETWEEN start_date AND end_date
    AND drl.status = 'completed'
    GROUP BY drl.execution_date
    ORDER BY drl.execution_date DESC;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 6. INDEXES FOR ARCHIVE TABLES
-- =====================================================

-- Indexes for archive tables to maintain query performance
CREATE INDEX idx_student_sessions_archive_created ON student_sessions_archive(created_at);
CREATE INDEX idx_student_sessions_archive_classroom ON student_sessions_archive(classroom_id);
CREATE INDEX idx_student_sessions_archive_archived ON student_sessions_archive(archived_at);

CREATE INDEX idx_student_progress_archive_created ON student_progress_archive(created_at);
CREATE INDEX idx_student_progress_archive_session ON student_progress_archive(student_session_id);
CREATE INDEX idx_student_progress_archive_archived ON student_progress_archive(archived_at);

CREATE INDEX idx_student_feedback_archive_submitted ON student_feedback_archive(submitted_at);
CREATE INDEX idx_student_feedback_archive_lesson ON student_feedback_archive(lesson_id);
CREATE INDEX idx_student_feedback_archive_archived ON student_feedback_archive(archived_at);

CREATE INDEX idx_classroom_sessions_archive_created ON classroom_sessions_archive(created_at);
CREATE INDEX idx_classroom_sessions_archive_classroom ON classroom_sessions_archive(classroom_id);
CREATE INDEX idx_classroom_sessions_archive_archived ON classroom_sessions_archive(archived_at);

CREATE INDEX idx_security_incidents_archive_created ON security_incidents_archive(created_at);
CREATE INDEX idx_security_incidents_archive_type ON security_incidents_archive(incident_type);
CREATE INDEX idx_security_incidents_archive_archived ON security_incidents_archive(archived_at);

CREATE INDEX idx_audit_log_archive_timestamp ON audit_log_archive(timestamp);
CREATE INDEX idx_audit_log_archive_table ON audit_log_archive(table_name);
CREATE INDEX idx_audit_log_archive_archived ON audit_log_archive(archived_at);

-- =====================================================
-- 7. INITIAL CONFIGURATION
-- =====================================================

-- Set up initial system configuration for data retention
INSERT INTO system_config (config_key, config_value, data_type, description) VALUES
('retention_job_enabled', 'true', 'boolean', 'Enable automated data retention job execution'),
('retention_job_schedule', '0 2 * * 0', 'string', 'Cron schedule for weekly retention job (Sundays at 2 AM)'),
('retention_notification_enabled', 'true', 'boolean', 'Enable notifications for retention job results'),
('retention_alert_threshold', '10000', 'integer', 'Alert threshold for large retention operations'),
('archive_compression_enabled', 'true', 'boolean', 'Enable compression for archived data'),
('retention_report_frequency', 'weekly', 'string', 'Frequency for retention reports'),
('retention_emergency_stop', 'false', 'boolean', 'Emergency stop flag for retention operations')
ON CONFLICT (config_key) DO NOTHING;

-- Log the retention policy setup completion
INSERT INTO audit_log (table_name, action, new_values, timestamp) VALUES
('retention_policies', 'SETUP_COMPLETE', 
 json_build_object(
    'policies_created', (SELECT COUNT(*) FROM retention_policies),
    'archive_tables_created', 6,
    'functions_created', 4,
    'setup_date', CURRENT_DATE
 ), CURRENT_TIMESTAMP);

-- Success message
SELECT 'Data retention policies successfully configured!' AS status,
       COUNT(*) AS policies_created,
       'Weekly execution schedule set' AS schedule_info
FROM retention_policies WHERE is_active = true;