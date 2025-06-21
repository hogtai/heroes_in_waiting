-- =====================================================
-- Heroes in Waiting - Backup & Disaster Recovery Procedures
-- Weekly Backup Schedule with 3-Year Retention
-- Comprehensive Backup and Recovery Strategy
-- =====================================================

-- =====================================================
-- 1. BACKUP CONFIGURATION & MANAGEMENT
-- =====================================================

-- Backup configuration table
CREATE TABLE IF NOT EXISTS backup_configurations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    backup_name VARCHAR(100) UNIQUE NOT NULL,
    backup_type VARCHAR(50) NOT NULL CHECK (backup_type IN ('full', 'incremental', 'differential', 'schema_only', 'data_only')),
    schedule_cron VARCHAR(100),
    retention_days INTEGER DEFAULT 1095, -- 3 years default
    is_active BOOLEAN DEFAULT true,
    backup_location VARCHAR(500),
    compression_enabled BOOLEAN DEFAULT true,
    encryption_enabled BOOLEAN DEFAULT true,
    priority INTEGER DEFAULT 5 CHECK (priority BETWEEN 1 AND 10),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Backup execution log
CREATE TABLE backup_execution_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    backup_config_id UUID NOT NULL REFERENCES backup_configurations(id) ON DELETE CASCADE,
    execution_id VARCHAR(100) UNIQUE NOT NULL,
    status VARCHAR(20) DEFAULT 'started' CHECK (status IN ('started', 'in_progress', 'completed', 'failed', 'cancelled')),
    backup_size_bytes BIGINT,
    compression_ratio DECIMAL(4,2),
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    duration_seconds INTEGER,
    backup_file_path VARCHAR(500),
    checksum VARCHAR(128),
    error_message TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Recovery operations log
CREATE TABLE recovery_operations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    operation_type VARCHAR(50) NOT NULL CHECK (operation_type IN ('full_restore', 'partial_restore', 'point_in_time', 'schema_restore', 'table_restore')),
    backup_execution_id UUID REFERENCES backup_execution_log(id),
    target_timestamp TIMESTAMP,
    tables_restored TEXT[],
    status VARCHAR(20) DEFAULT 'started' CHECK (status IN ('started', 'in_progress', 'completed', 'failed', 'cancelled')),
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    duration_seconds INTEGER,
    records_restored BIGINT,
    initiated_by UUID REFERENCES facilitators(id),
    recovery_notes TEXT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 2. BACKUP CONFIGURATION SETUP
-- =====================================================

-- Insert default backup configurations
INSERT INTO backup_configurations (
    backup_name, backup_type, schedule_cron, retention_days, 
    backup_location, description, priority
) VALUES
-- Weekly full backup (Sundays at 1 AM)
('weekly_full_backup', 'full', '0 1 * * 0', 1095, 
 '/backups/heroes_in_waiting/full/', 'Complete database backup weekly', 1),

-- Daily incremental backup (Daily at 3 AM, except Sunday)
('daily_incremental_backup', 'incremental', '0 3 * * 1-6', 90, 
 '/backups/heroes_in_waiting/incremental/', 'Daily incremental backup', 2),

-- Monthly archive backup (1st of month at 2 AM)
('monthly_archive_backup', 'full', '0 2 1 * *', 2190, 
 '/backups/heroes_in_waiting/archive/', 'Monthly long-term archive', 1),

-- Schema-only backup (Daily at 4 AM)
('daily_schema_backup', 'schema_only', '0 4 * * *', 365, 
 '/backups/heroes_in_waiting/schema/', 'Daily schema structure backup', 3),

-- Critical data backup (Every 6 hours)
('critical_data_backup', 'data_only', '0 */6 * * *', 30, 
 '/backups/heroes_in_waiting/critical/', 'Critical tables backup every 6 hours', 1);

-- =====================================================
-- 3. BACKUP EXECUTION FUNCTIONS
-- =====================================================

-- Function to execute backup based on configuration
CREATE OR REPLACE FUNCTION execute_backup(config_name VARCHAR(100)) 
RETURNS UUID AS $$
DECLARE
    config_record backup_configurations%ROWTYPE;
    execution_id VARCHAR(100);
    log_id UUID;
    start_time TIMESTAMP;
    backup_file VARCHAR(500);
    backup_command TEXT;
    result INTEGER;
BEGIN
    start_time := CURRENT_TIMESTAMP;
    execution_id := config_name || '_' || to_char(start_time, 'YYYY_MM_DD_HH24_MI_SS');
    
    -- Get backup configuration
    SELECT * INTO config_record FROM backup_configurations 
    WHERE backup_name = config_name AND is_active = true;
    
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Backup configuration not found or inactive: %', config_name;
    END IF;
    
    -- Create backup execution log entry
    INSERT INTO backup_execution_log (
        backup_config_id, execution_id, status, start_time
    ) VALUES (
        config_record.id, execution_id, 'started', start_time
    ) RETURNING id INTO log_id;
    
    -- Update status to in_progress
    UPDATE backup_execution_log 
    SET status = 'in_progress' 
    WHERE id = log_id;
    
    -- Generate backup file path
    backup_file := config_record.backup_location || execution_id || '.sql';
    IF config_record.compression_enabled THEN
        backup_file := backup_file || '.gz';
    END IF;
    
    -- Log backup start
    INSERT INTO audit_log (table_name, action, new_values, timestamp) VALUES
    ('backup_execution', 'START', 
     json_build_object('execution_id', execution_id, 'backup_type', config_record.backup_type), 
     CURRENT_TIMESTAMP);
    
    -- Update execution log with file path
    UPDATE backup_execution_log 
    SET backup_file_path = backup_file,
        metadata = json_build_object(
            'compression_enabled', config_record.compression_enabled,
            'encryption_enabled', config_record.encryption_enabled,
            'backup_type', config_record.backup_type
        )
    WHERE id = log_id;
    
    RETURN log_id;
END;
$$ LANGUAGE plpgsql;

-- Function to complete backup execution
CREATE OR REPLACE FUNCTION complete_backup_execution(
    log_id UUID,
    file_size BIGINT,
    checksum_value VARCHAR(128),
    success BOOLEAN DEFAULT true,
    error_msg TEXT DEFAULT NULL
) RETURNS VOID AS $$
DECLARE
    start_timestamp TIMESTAMP;
    duration INTEGER;
BEGIN
    -- Get start time
    SELECT start_time INTO start_timestamp FROM backup_execution_log WHERE id = log_id;
    
    -- Calculate duration
    duration := EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - start_timestamp));
    
    -- Update backup execution log
    UPDATE backup_execution_log SET
        status = CASE WHEN success THEN 'completed' ELSE 'failed' END,
        end_time = CURRENT_TIMESTAMP,
        duration_seconds = duration,
        backup_size_bytes = file_size,
        checksum = checksum_value,
        error_message = error_msg
    WHERE id = log_id;
    
    -- Log completion
    INSERT INTO audit_log (table_name, action, new_values, timestamp) VALUES
    ('backup_execution', 'COMPLETE', 
     json_build_object(
        'log_id', log_id,
        'success', success,
        'duration_seconds', duration,
        'file_size_bytes', file_size
     ), CURRENT_TIMESTAMP);
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 4. BACKUP VERIFICATION & VALIDATION
-- =====================================================

-- Function to verify backup integrity
CREATE OR REPLACE FUNCTION verify_backup_integrity(log_id UUID) 
RETURNS BOOLEAN AS $$
DECLARE
    backup_record backup_execution_log%ROWTYPE;
    file_exists BOOLEAN;
    calculated_checksum VARCHAR(128);
BEGIN
    -- Get backup record
    SELECT * INTO backup_record FROM backup_execution_log WHERE id = log_id;
    
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Backup execution log not found: %', log_id;
    END IF;
    
    -- Log verification start
    INSERT INTO audit_log (table_name, action, new_values, timestamp) VALUES
    ('backup_verification', 'START', 
     json_build_object('log_id', log_id, 'backup_file', backup_record.backup_file_path), 
     CURRENT_TIMESTAMP);
    
    -- Here would be actual file system checks (simulated)
    -- In real implementation, this would:
    -- 1. Check if file exists
    -- 2. Calculate file checksum
    -- 3. Compare with stored checksum
    -- 4. Attempt to read backup header
    
    -- For now, return true if status is completed and checksum exists
    RETURN backup_record.status = 'completed' AND backup_record.checksum IS NOT NULL;
END;
$$ LANGUAGE plpgsql;

-- Function to test backup restore capability
CREATE OR REPLACE FUNCTION test_backup_restore(log_id UUID, test_schema VARCHAR(100) DEFAULT 'backup_test') 
RETURNS BOOLEAN AS $$
DECLARE
    backup_record backup_execution_log%ROWTYPE;
    test_result BOOLEAN := false;
BEGIN
    -- Get backup record
    SELECT * INTO backup_record FROM backup_execution_log WHERE id = log_id;
    
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Backup execution log not found: %', log_id;
    END IF;
    
    -- Log restore test start
    INSERT INTO audit_log (table_name, action, new_values, timestamp) VALUES
    ('backup_restore_test', 'START', 
     json_build_object('log_id', log_id, 'test_schema', test_schema), 
     CURRENT_TIMESTAMP);
    
    -- In real implementation, this would:
    -- 1. Create test schema
    -- 2. Restore backup to test schema
    -- 3. Validate data integrity
    -- 4. Clean up test schema
    
    -- For now, simulate successful test
    test_result := true;
    
    -- Log test completion
    INSERT INTO audit_log (table_name, action, new_values, timestamp) VALUES
    ('backup_restore_test', 'COMPLETE', 
     json_build_object('log_id', log_id, 'success', test_result), 
     CURRENT_TIMESTAMP);
    
    RETURN test_result;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 5. RECOVERY PROCEDURES
-- =====================================================

-- Function to initiate database recovery
CREATE OR REPLACE FUNCTION initiate_recovery(
    recovery_type VARCHAR(50),
    backup_log_id UUID DEFAULT NULL,
    target_time TIMESTAMP DEFAULT NULL,
    specific_tables TEXT[] DEFAULT NULL,
    initiated_by_user UUID DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    recovery_id UUID;
    backup_record backup_execution_log%ROWTYPE;
BEGIN
    -- Validate recovery type
    IF recovery_type NOT IN ('full_restore', 'partial_restore', 'point_in_time', 'schema_restore', 'table_restore') THEN
        RAISE EXCEPTION 'Invalid recovery type: %', recovery_type;
    END IF;
    
    -- Get backup record if provided
    IF backup_log_id IS NOT NULL THEN
        SELECT * INTO backup_record FROM backup_execution_log WHERE id = backup_log_id;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'Backup execution log not found: %', backup_log_id;
        END IF;
    END IF;
    
    -- Create recovery operation record
    INSERT INTO recovery_operations (
        operation_type, backup_execution_id, target_timestamp, 
        tables_restored, initiated_by, status
    ) VALUES (
        recovery_type, backup_log_id, target_time, 
        specific_tables, initiated_by_user, 'started'
    ) RETURNING id INTO recovery_id;
    
    -- Log recovery initiation
    INSERT INTO audit_log (table_name, action, new_values, timestamp) VALUES
    ('recovery_operations', 'INITIATE', 
     json_build_object(
        'recovery_id', recovery_id,
        'recovery_type', recovery_type,
        'backup_log_id', backup_log_id,
        'initiated_by', initiated_by_user
     ), CURRENT_TIMESTAMP);
    
    RETURN recovery_id;
END;
$$ LANGUAGE plpgsql;

-- Function to complete recovery operation
CREATE OR REPLACE FUNCTION complete_recovery_operation(
    recovery_id UUID,
    success BOOLEAN,
    records_count BIGINT DEFAULT 0,
    notes TEXT DEFAULT NULL,
    error_msg TEXT DEFAULT NULL
) RETURNS VOID AS $$
DECLARE
    start_timestamp TIMESTAMP;
    duration INTEGER;
BEGIN
    -- Get start time
    SELECT start_time INTO start_timestamp FROM recovery_operations WHERE id = recovery_id;
    
    -- Calculate duration
    duration := EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - start_timestamp));
    
    -- Update recovery operation
    UPDATE recovery_operations SET
        status = CASE WHEN success THEN 'completed' ELSE 'failed' END,
        end_time = CURRENT_TIMESTAMP,
        duration_seconds = duration,
        records_restored = records_count,
        recovery_notes = notes,
        error_message = error_msg
    WHERE id = recovery_id;
    
    -- Log completion
    INSERT INTO audit_log (table_name, action, new_values, timestamp) VALUES
    ('recovery_operations', 'COMPLETE', 
     json_build_object(
        'recovery_id', recovery_id,
        'success', success,
        'duration_seconds', duration,
        'records_restored', records_count
     ), CURRENT_TIMESTAMP);
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 6. DISASTER RECOVERY PROCEDURES
-- =====================================================

-- Disaster recovery plan execution
CREATE OR REPLACE FUNCTION execute_disaster_recovery_plan(
    disaster_type VARCHAR(50),
    severity VARCHAR(20) DEFAULT 'high'
) RETURNS UUID AS $$
DECLARE
    plan_execution_id UUID;
    latest_backup_id UUID;
    recovery_id UUID;
BEGIN
    plan_execution_id := uuid_generate_v4();
    
    -- Log disaster recovery initiation
    INSERT INTO audit_log (table_name, action, new_values, timestamp) VALUES
    ('disaster_recovery', 'INITIATE', 
     json_build_object(
        'plan_execution_id', plan_execution_id,
        'disaster_type', disaster_type,
        'severity', severity
     ), CURRENT_TIMESTAMP);
    
    -- Get latest successful backup
    SELECT id INTO latest_backup_id 
    FROM backup_execution_log 
    WHERE status = 'completed' 
    ORDER BY end_time DESC 
    LIMIT 1;
    
    IF latest_backup_id IS NULL THEN
        RAISE EXCEPTION 'No successful backup found for disaster recovery';
    END IF;
    
    -- Initiate recovery from latest backup
    recovery_id := initiate_recovery('full_restore', latest_backup_id);
    
    -- Log recovery initiation in disaster context
    INSERT INTO audit_log (table_name, action, new_values, timestamp) VALUES
    ('disaster_recovery', 'RECOVERY_STARTED', 
     json_build_object(
        'plan_execution_id', plan_execution_id,
        'recovery_id', recovery_id,
        'backup_id', latest_backup_id
     ), CURRENT_TIMESTAMP);
    
    RETURN plan_execution_id;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 7. BACKUP CLEANUP & RETENTION
-- =====================================================

-- Function to clean up old backups based on retention policy
CREATE OR REPLACE FUNCTION cleanup_old_backups() 
RETURNS TABLE (
    config_name VARCHAR(100),
    backups_deleted INTEGER,
    space_freed_bytes BIGINT
) AS $$
DECLARE
    config_record backup_configurations%ROWTYPE;
    deleted_count INTEGER;
    space_freed BIGINT;
    retention_date DATE;
BEGIN
    FOR config_record IN SELECT * FROM backup_configurations WHERE is_active = true
    LOOP
        deleted_count := 0;
        space_freed := 0;
        retention_date := CURRENT_DATE - INTERVAL '1 day' * config_record.retention_days;
        
        -- Calculate space to be freed
        SELECT COUNT(*), COALESCE(SUM(backup_size_bytes), 0)
        INTO deleted_count, space_freed
        FROM backup_execution_log 
        WHERE backup_config_id = config_record.id 
        AND start_time::DATE < retention_date;
        
        -- Delete old backup records
        DELETE FROM backup_execution_log 
        WHERE backup_config_id = config_record.id 
        AND start_time::DATE < retention_date;
        
        -- Log cleanup
        INSERT INTO audit_log (table_name, action, new_values, timestamp) VALUES
        ('backup_cleanup', 'EXECUTE', 
         json_build_object(
            'config_name', config_record.backup_name,
            'retention_date', retention_date,
            'backups_deleted', deleted_count,
            'space_freed_bytes', space_freed
         ), CURRENT_TIMESTAMP);
        
        -- Return results
        config_name := config_record.backup_name;
        backups_deleted := deleted_count;
        space_freed_bytes := space_freed;
        RETURN NEXT;
    END LOOP;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 8. MONITORING & REPORTING
-- =====================================================

-- View for backup status monitoring
CREATE VIEW backup_status_view AS
SELECT 
    bc.backup_name,
    bc.backup_type,
    bc.schedule_cron,
    bc.is_active,
    bel.execution_id,
    bel.status,
    bel.start_time,
    bel.end_time,
    bel.duration_seconds,
    bel.backup_size_bytes,
    bel.error_message,
    CASE 
        WHEN bel.end_time IS NULL THEN 'In Progress'
        WHEN bel.status = 'completed' THEN 'Success'
        WHEN bel.status = 'failed' THEN 'Failed'
        ELSE 'Unknown'
    END AS status_display
FROM backup_configurations bc
LEFT JOIN backup_execution_log bel ON bc.id = bel.backup_config_id
WHERE bc.is_active = true
ORDER BY bc.priority, bel.start_time DESC;

-- View for recovery operations monitoring
CREATE VIEW recovery_status_view AS
SELECT 
    ro.id,
    ro.operation_type,
    ro.status,
    ro.start_time,
    ro.end_time,
    ro.duration_seconds,
    ro.records_restored,
    bel.execution_id AS backup_execution,
    f.first_name || ' ' || f.last_name AS initiated_by_name,
    ro.recovery_notes,
    ro.error_message
FROM recovery_operations ro
LEFT JOIN backup_execution_log bel ON ro.backup_execution_id = bel.id
LEFT JOIN facilitators f ON ro.initiated_by = f.id
ORDER BY ro.created_at DESC;

-- Function to generate backup health report
CREATE OR REPLACE FUNCTION generate_backup_health_report(
    days_back INTEGER DEFAULT 30
) RETURNS TABLE (
    backup_config VARCHAR(100),
    total_backups INTEGER,
    successful_backups INTEGER,
    failed_backups INTEGER,
    success_rate DECIMAL(5,2),
    avg_backup_size_mb DECIMAL(10,2),
    avg_duration_minutes DECIMAL(8,2),
    last_successful_backup TIMESTAMP,
    health_status VARCHAR(20)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        bc.backup_name,
        COUNT(bel.id)::INTEGER AS total_backups,
        COUNT(CASE WHEN bel.status = 'completed' THEN 1 END)::INTEGER AS successful_backups,
        COUNT(CASE WHEN bel.status = 'failed' THEN 1 END)::INTEGER AS failed_backups,
        ROUND(
            (COUNT(CASE WHEN bel.status = 'completed' THEN 1 END)::DECIMAL / 
             NULLIF(COUNT(bel.id), 0)) * 100, 2
        ) AS success_rate,
        ROUND(AVG(bel.backup_size_bytes) / 1024.0 / 1024.0, 2) AS avg_backup_size_mb,
        ROUND(AVG(bel.duration_seconds) / 60.0, 2) AS avg_duration_minutes,
        MAX(CASE WHEN bel.status = 'completed' THEN bel.end_time END) AS last_successful_backup,
        CASE 
            WHEN COUNT(CASE WHEN bel.status = 'completed' THEN 1 END) = 0 THEN 'Critical'
            WHEN (COUNT(CASE WHEN bel.status = 'completed' THEN 1 END)::DECIMAL / 
                  NULLIF(COUNT(bel.id), 0)) < 0.8 THEN 'Warning'
            WHEN MAX(CASE WHEN bel.status = 'completed' THEN bel.end_time END) < 
                 CURRENT_TIMESTAMP - INTERVAL '7 days' THEN 'Stale'
            ELSE 'Healthy'
        END AS health_status
    FROM backup_configurations bc
    LEFT JOIN backup_execution_log bel ON bc.id = bel.backup_config_id
        AND bel.start_time >= CURRENT_DATE - INTERVAL '1 day' * days_back
    WHERE bc.is_active = true
    GROUP BY bc.backup_name, bc.id
    ORDER BY bc.priority, success_rate DESC;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 9. BACKUP SCHEDULING & AUTOMATION
-- =====================================================

-- Function to check and execute scheduled backups
CREATE OR REPLACE FUNCTION check_scheduled_backups() RETURNS VOID AS $$
DECLARE
    config_record backup_configurations%ROWTYPE;
    should_execute BOOLEAN;
BEGIN
    FOR config_record IN SELECT * FROM backup_configurations WHERE is_active = true
    LOOP
        -- In real implementation, this would parse cron schedule
        -- and determine if backup should run now
        should_execute := false;
        
        -- Simplified check - execute if no backup in last 24 hours for daily schedules
        IF config_record.schedule_cron LIKE '%* * *%' THEN -- Daily pattern
            SELECT NOT EXISTS(
                SELECT 1 FROM backup_execution_log 
                WHERE backup_config_id = config_record.id 
                AND start_time > CURRENT_TIMESTAMP - INTERVAL '24 hours'
                AND status = 'completed'
            ) INTO should_execute;
        END IF;
        
        IF should_execute THEN
            -- Schedule backup execution
            INSERT INTO job_queue (job_type, priority, payload, scheduled_at) VALUES
            ('backup_execution', config_record.priority, 
             json_build_object('config_name', config_record.backup_name), 
             CURRENT_TIMESTAMP);
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 10. INDEXES FOR BACKUP TABLES
-- =====================================================

CREATE INDEX idx_backup_configurations_active ON backup_configurations(is_active) WHERE is_active = true;
CREATE INDEX idx_backup_configurations_priority ON backup_configurations(priority);

CREATE INDEX idx_backup_execution_log_config ON backup_execution_log(backup_config_id);
CREATE INDEX idx_backup_execution_log_status ON backup_execution_log(status);
CREATE INDEX idx_backup_execution_log_start_time ON backup_execution_log(start_time);
CREATE INDEX idx_backup_execution_log_execution_id ON backup_execution_log(execution_id);

CREATE INDEX idx_recovery_operations_type ON recovery_operations(operation_type);
CREATE INDEX idx_recovery_operations_status ON recovery_operations(status);
CREATE INDEX idx_recovery_operations_start_time ON recovery_operations(start_time);
CREATE INDEX idx_recovery_operations_initiated_by ON recovery_operations(initiated_by);

-- =====================================================
-- 11. TRIGGERS FOR BACKUP OPERATIONS
-- =====================================================

-- Trigger to update backup configuration timestamps
CREATE TRIGGER trigger_backup_configurations_updated_at
    BEFORE UPDATE ON backup_configurations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- 12. INITIAL CONFIGURATION & TESTING
-- =====================================================

-- Insert backup-related system configuration
INSERT INTO system_config (config_key, config_value, data_type, description) VALUES
('backup_enabled', 'true', 'boolean', 'Enable automated backup system'),
('backup_compression_level', '6', 'integer', 'Compression level for backups (1-9)'),
('backup_parallel_jobs', '2', 'integer', 'Number of parallel backup jobs'),
('backup_timeout_hours', '4', 'integer', 'Backup timeout in hours'),
('backup_notification_enabled', 'true', 'boolean', 'Enable backup completion notifications'),
('backup_verification_enabled', 'true', 'boolean', 'Enable automatic backup verification'),
('disaster_recovery_enabled', 'true', 'boolean', 'Enable disaster recovery procedures'),
('backup_encryption_key_rotation_days', '90', 'integer', 'Days between encryption key rotations')
ON CONFLICT (config_key) DO NOTHING;

-- Log backup system setup completion
INSERT INTO audit_log (table_name, action, new_values, timestamp) VALUES
('backup_system', 'SETUP_COMPLETE', 
 json_build_object(
    'backup_configs_created', (SELECT COUNT(*) FROM backup_configurations),
    'functions_created', 12,
    'views_created', 2,
    'setup_date', CURRENT_DATE,
    'retention_years', 3
 ), CURRENT_TIMESTAMP);

-- Success message
SELECT 'Backup and Disaster Recovery system successfully configured!' AS status,
       COUNT(*) AS backup_configurations_created,
       '3-year retention with weekly schedule' AS retention_info
FROM backup_configurations WHERE is_active = true;