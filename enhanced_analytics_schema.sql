-- =====================================================
-- Heroes in Waiting - Enhanced Analytics Database Schema
-- Checkpoint 6 Phase 2: Database Testing Implementation
-- Date: 2025-07-10
-- =====================================================

-- Enable required extensions for analytics
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- =====================================================
-- 1. ENHANCED BEHAVIORAL ANALYTICS TRACKING
-- =====================================================

-- Anonymous behavioral analytics for educational insights
CREATE TABLE behavioral_analytics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_session_id UUID NOT NULL REFERENCES student_sessions(id) ON DELETE CASCADE,
    classroom_id UUID NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE,
    lesson_id UUID REFERENCES lessons(id) ON DELETE CASCADE,
    
    -- Anonymous tracking identifiers (COPPA compliant)
    anonymous_student_hash VARCHAR(64) NOT NULL, -- SHA-256 hash with daily salt
    session_tracking_id VARCHAR(64) NOT NULL,    -- Session-specific tracking
    
    -- Behavioral categories (Heroes in Waiting focus areas)
    behavioral_category VARCHAR(50) NOT NULL CHECK (
        behavioral_category IN ('empathy', 'confidence', 'communication', 'leadership', 'kindness', 'courage')
    ),
    
    -- Interaction details
    interaction_type VARCHAR(100) NOT NULL, -- peer_help, active_listening, speaking_up, etc.
    interaction_context VARCHAR(200),       -- peer_distress, group_activity, discussion, etc.
    behavioral_score INTEGER CHECK (behavioral_score BETWEEN 1 AND 5), -- 1-5 scoring scale
    
    -- Engagement metrics
    engagement_level VARCHAR(20) CHECK (engagement_level IN ('low', 'medium', 'high', 'exceptional')),
    emotional_response VARCHAR(50),          -- excited, thoughtful, concerned, empathetic, etc.
    peer_interaction_count INTEGER DEFAULT 0,
    
    -- Educational context
    learning_objective VARCHAR(200),         -- Related lesson objective
    skill_demonstration TEXT,               -- How the behavior demonstrates skill growth
    growth_indicator BOOLEAN DEFAULT false, -- Indicates significant growth moment
    
    -- Timing and duration
    interaction_duration_seconds INTEGER,
    lesson_segment VARCHAR(100),            -- video, activity, discussion, reflection
    
    -- Metadata for research and analysis
    metadata JSONB,                         -- Additional structured data
    
    -- Timestamps
    tracked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 2. LESSON EFFECTIVENESS MEASUREMENT
-- =====================================================

-- Curriculum impact measurement and improvement tracking
CREATE TABLE lesson_effectiveness (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    classroom_id UUID NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE,
    measurement_date DATE NOT NULL,
    
    -- Pre/post assessment data (aggregated, anonymous)
    pre_assessment_scores JSONB,           -- Anonymous pre-lesson skill assessments
    post_assessment_scores JSONB,          -- Anonymous post-lesson skill assessments
    skill_improvement_percentage DECIMAL(5,2), -- Overall improvement percentage
    
    -- Engagement metrics
    average_engagement_score DECIMAL(4,2) CHECK (average_engagement_score BETWEEN 1.0 AND 5.0),
    completion_rate DECIMAL(5,2) CHECK (completion_rate BETWEEN 0.0 AND 100.0),
    time_on_task_minutes INTEGER,
    interaction_quality_score DECIMAL(4,2),
    
    -- Learning outcomes achieved
    learning_objectives_met JSONB,         -- Which objectives were successfully met
    behavioral_demonstrations JSONB,       -- Observed behavioral improvements
    peer_collaboration_score DECIMAL(4,2),
    empathy_indicators_count INTEGER DEFAULT 0,
    
    -- Content effectiveness
    content_section_effectiveness JSONB,   -- Effectiveness by content section
    most_engaging_content VARCHAR(200),
    least_engaging_content VARCHAR(200),
    content_difficulty_rating DECIMAL(4,2),
    
    -- Facilitator feedback
    facilitator_rating DECIMAL(4,2) CHECK (facilitator_rating BETWEEN 1.0 AND 5.0),
    facilitator_notes TEXT,
    implementation_quality VARCHAR(20) CHECK (implementation_quality IN ('excellent', 'good', 'fair', 'needs_improvement')),
    
    -- Recommendations and insights
    improvement_recommendations JSONB,
    next_lesson_readiness BOOLEAN DEFAULT true,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 3. TIME SERIES ANALYTICS FOR LONGITUDINAL STUDIES
-- =====================================================

-- Trend analysis and longitudinal educational research data
CREATE TABLE time_series_analytics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    
    -- Time dimensions
    measurement_timestamp TIMESTAMP NOT NULL,
    time_bucket VARCHAR(20) NOT NULL CHECK (time_bucket IN ('hourly', 'daily', 'weekly', 'monthly')),
    academic_period VARCHAR(50),            -- semester, quarter, school_year
    
    -- Scope identifiers (various aggregation levels)
    classroom_id UUID REFERENCES classrooms(id) ON DELETE CASCADE,
    lesson_id UUID REFERENCES lessons(id) ON DELETE CASCADE,
    behavioral_category VARCHAR(50),
    
    -- Aggregated metrics (anonymous, no individual student data)
    student_count INTEGER NOT NULL,
    total_interactions INTEGER DEFAULT 0,
    average_engagement_score DECIMAL(4,2),
    behavioral_growth_indicators INTEGER DEFAULT 0,
    
    -- Trend data
    empathy_score_trend JSONB,             -- Historical empathy development
    confidence_score_trend JSONB,          -- Confidence building over time
    communication_score_trend JSONB,       -- Communication skill development
    leadership_score_trend JSONB,          -- Leadership behavior growth
    
    -- Comparative analysis
    period_over_period_change DECIMAL(5,2), -- Percentage change from previous period
    benchmark_comparison DECIMAL(5,2),      -- Comparison to program benchmarks
    peer_classroom_ranking INTEGER,         -- Anonymous ranking among similar classrooms
    
    -- Statistical measures
    standard_deviation DECIMAL(6,3),
    confidence_interval_lower DECIMAL(4,2),
    confidence_interval_upper DECIMAL(4,2),
    statistical_significance BOOLEAN DEFAULT false,
    
    -- Research metadata
    sample_size INTEGER,
    data_quality_score DECIMAL(4,2),       -- Quality assessment of underlying data
    research_notes TEXT,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 4. EDUCATIONAL IMPACT METRICS FOR RESEARCH
-- =====================================================

-- Program evaluation and educational research data
CREATE TABLE educational_impact_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    
    -- Research study context
    study_period_start DATE NOT NULL,
    study_period_end DATE NOT NULL,
    study_cohort_identifier VARCHAR(100),   -- Anonymous cohort identification
    
    -- Scope and demographics (anonymous)
    classroom_count INTEGER NOT NULL,
    total_student_count INTEGER NOT NULL,
    age_range_distribution JSONB,          -- Distribution across age ranges
    grade_level_distribution JSONB,        -- Distribution across grade levels
    
    -- Core educational outcomes
    empathy_development_score DECIMAL(5,2), -- Overall empathy growth measurement
    leadership_behavior_frequency DECIMAL(5,2), -- Leadership behaviors per session
    peer_helping_incidents INTEGER DEFAULT 0,
    conflict_resolution_success_rate DECIMAL(5,2),
    inclusive_behavior_indicators INTEGER DEFAULT 0,
    
    -- Communication and social skills
    active_listening_demonstrations INTEGER DEFAULT 0,
    constructive_communication_score DECIMAL(4,2),
    collaboration_quality_rating DECIMAL(4,2),
    peer_support_interactions INTEGER DEFAULT 0,
    
    -- Curriculum effectiveness measures
    lesson_completion_rate DECIMAL(5,2),
    objective_achievement_rate DECIMAL(5,2),
    content_retention_score DECIMAL(4,2),
    skill_transfer_evidence JSONB,         -- Evidence of skill application
    
    -- Long-term impact indicators
    sustained_behavior_change BOOLEAN DEFAULT false,
    peer_nomination_improvements INTEGER DEFAULT 0,
    facilitator_observed_growth JSONB,
    classroom_climate_improvement DECIMAL(4,2),
    
    -- Program comparison metrics
    control_group_comparison DECIMAL(5,2), -- Comparison to control groups
    pre_post_improvement_percentage DECIMAL(5,2),
    benchmark_achievement_level VARCHAR(20) CHECK (
        benchmark_achievement_level IN ('below_benchmark', 'meets_benchmark', 'exceeds_benchmark', 'exceptional')
    ),
    
    -- Research validity measures
    data_collection_completeness DECIMAL(5,2),
    inter_rater_reliability DECIMAL(4,2),
    statistical_power DECIMAL(4,2),
    effect_size DECIMAL(4,2),
    
    -- External validation
    educator_feedback_score DECIMAL(4,2),
    parent_reported_changes JSONB,         -- Anonymous parent feedback aggregation
    peer_assessment_improvements DECIMAL(4,2),
    
    -- Research metadata
    methodology_notes TEXT,
    limitations_identified JSONB,
    recommendations JSONB,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 5. ANALYTICS AGGREGATION CACHE FOR PERFORMANCE
-- =====================================================

-- Real-time dashboard optimization with materialized data
CREATE TABLE analytics_aggregation_cache (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    
    -- Cache identification
    cache_key VARCHAR(255) UNIQUE NOT NULL,
    cache_type VARCHAR(50) NOT NULL CHECK (
        cache_type IN ('classroom_dashboard', 'facilitator_overview', 'lesson_analytics', 'behavioral_summary', 'research_export')
    ),
    
    -- Scope identifiers
    facilitator_id UUID REFERENCES facilitators(id) ON DELETE CASCADE,
    classroom_id UUID REFERENCES classrooms(id) ON DELETE CASCADE,
    lesson_id UUID REFERENCES lessons(id) ON DELETE CASCADE,
    
    -- Time range for cached data
    data_start_date DATE NOT NULL,
    data_end_date DATE NOT NULL,
    aggregation_level VARCHAR(20) NOT NULL CHECK (
        aggregation_level IN ('daily', 'weekly', 'monthly', 'semester', 'yearly')
    ),
    
    -- Cached analytics data
    aggregated_data JSONB NOT NULL,        -- Pre-computed analytics for fast retrieval
    summary_statistics JSONB,              -- Quick summary stats
    chart_data JSONB,                      -- Pre-computed chart data for dashboards
    export_data JSONB,                     -- Pre-formatted export data
    
    -- Cache metadata
    record_count INTEGER DEFAULT 0,
    computation_time_ms INTEGER,           -- Time taken to compute cache
    data_freshness_score DECIMAL(4,2),     -- How fresh the underlying data is
    
    -- Cache management
    cache_status VARCHAR(20) DEFAULT 'active' CHECK (
        cache_status IN ('active', 'stale', 'expired', 'computing', 'error')
    ),
    cache_priority INTEGER DEFAULT 5 CHECK (cache_priority BETWEEN 1 AND 10),
    access_count INTEGER DEFAULT 0,
    last_accessed_at TIMESTAMP,
    
    -- Refresh management
    auto_refresh_enabled BOOLEAN DEFAULT true,
    refresh_frequency_minutes INTEGER DEFAULT 60,
    next_refresh_at TIMESTAMP,
    last_refresh_duration_ms INTEGER,
    
    -- Error handling
    last_error_message TEXT,
    error_count INTEGER DEFAULT 0,
    
    -- Timestamps
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 6. ANONYMOUS HASHING FUNCTIONS FOR COPPA COMPLIANCE
-- =====================================================

-- Daily salt rotation for anonymous student identifiers
CREATE TABLE anonymous_hash_salts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    salt_date DATE UNIQUE NOT NULL,
    salt_value VARCHAR(64) NOT NULL,       -- Random salt for daily hash generation
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Generate anonymous student hash with daily salt
CREATE OR REPLACE FUNCTION generate_anonymous_student_hash(
    student_identifier TEXT,
    hash_date DATE DEFAULT CURRENT_DATE
) RETURNS VARCHAR(64) AS $$
DECLARE
    daily_salt VARCHAR(64);
    anonymous_hash VARCHAR(64);
BEGIN
    -- Get or create daily salt
    SELECT salt_value INTO daily_salt 
    FROM anonymous_hash_salts 
    WHERE salt_date = hash_date AND is_active = true;
    
    IF daily_salt IS NULL THEN
        -- Generate new salt for the date
        daily_salt := encode(gen_random_bytes(32), 'hex');
        INSERT INTO anonymous_hash_salts (salt_date, salt_value)
        VALUES (hash_date, daily_salt)
        ON CONFLICT (salt_date) DO UPDATE SET salt_value = EXCLUDED.salt_value;
    END IF;
    
    -- Generate SHA-256 hash with salt
    anonymous_hash := encode(
        digest(student_identifier || daily_salt, 'sha256'), 
        'hex'
    );
    
    RETURN anonymous_hash;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Validate anonymous hash consistency
CREATE OR REPLACE FUNCTION validate_anonymous_hash_consistency(
    student_identifier TEXT,
    expected_hash VARCHAR(64),
    hash_date DATE DEFAULT CURRENT_DATE
) RETURNS BOOLEAN AS $$
DECLARE
    computed_hash VARCHAR(64);
BEGIN
    computed_hash := generate_anonymous_student_hash(student_identifier, hash_date);
    RETURN computed_hash = expected_hash;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- 7. ENGAGEMENT SCORING ALGORITHMS
-- =====================================================

-- Calculate behavioral engagement score
CREATE OR REPLACE FUNCTION calculate_behavioral_engagement_score(
    p_student_session_id UUID,
    p_lesson_id UUID DEFAULT NULL
) RETURNS DECIMAL(4,2) AS $$
DECLARE
    engagement_score DECIMAL(4,2) := 0.0;
    interaction_count INTEGER := 0;
    avg_behavioral_score DECIMAL(4,2);
    empathy_bonus DECIMAL(2,2) := 0.0;
    leadership_bonus DECIMAL(2,2) := 0.0;
BEGIN
    -- Get base metrics from behavioral analytics
    SELECT 
        COUNT(*),
        AVG(behavioral_score)
    INTO interaction_count, avg_behavioral_score
    FROM behavioral_analytics
    WHERE student_session_id = p_student_session_id
    AND (p_lesson_id IS NULL OR lesson_id = p_lesson_id);
    
    -- Base score from average behavioral scores
    engagement_score := COALESCE(avg_behavioral_score, 0.0);
    
    -- Bonus for empathy demonstrations
    SELECT COUNT(*) * 0.1 INTO empathy_bonus
    FROM behavioral_analytics
    WHERE student_session_id = p_student_session_id
    AND (p_lesson_id IS NULL OR lesson_id = p_lesson_id)
    AND behavioral_category = 'empathy'
    AND behavioral_score >= 4;
    
    -- Bonus for leadership behaviors
    SELECT COUNT(*) * 0.1 INTO leadership_bonus
    FROM behavioral_analytics
    WHERE student_session_id = p_student_session_id
    AND (p_lesson_id IS NULL OR lesson_id = p_lesson_id)
    AND behavioral_category = 'leadership'
    AND behavioral_score >= 4;
    
    -- Calculate final engagement score (max 5.0)
    engagement_score := LEAST(5.0, engagement_score + empathy_bonus + leadership_bonus);
    
    RETURN engagement_score;
END;
$$ LANGUAGE plpgsql;

-- Calculate lesson effectiveness score
CREATE OR REPLACE FUNCTION calculate_lesson_effectiveness_score(
    p_lesson_id UUID,
    p_classroom_id UUID DEFAULT NULL
) RETURNS DECIMAL(4,2) AS $$
DECLARE
    effectiveness_score DECIMAL(4,2) := 0.0;
    avg_engagement DECIMAL(4,2);
    completion_rate DECIMAL(5,2);
    behavioral_growth INTEGER;
    objective_achievement DECIMAL(5,2);
BEGIN
    -- Get aggregated lesson metrics
    SELECT 
        AVG(average_engagement_score),
        AVG(completion_rate),
        SUM(behavioral_demonstrations->>'growth_indicators')::INTEGER,
        AVG((learning_objectives_met->>'achievement_percentage')::DECIMAL)
    INTO avg_engagement, completion_rate, behavioral_growth, objective_achievement
    FROM lesson_effectiveness
    WHERE lesson_id = p_lesson_id
    AND (p_classroom_id IS NULL OR classroom_id = p_classroom_id);
    
    -- Calculate weighted effectiveness score
    effectiveness_score := (
        COALESCE(avg_engagement, 0.0) * 0.3 +           -- 30% engagement
        COALESCE(completion_rate, 0.0) / 20.0 +         -- 30% completion (scaled)
        LEAST(5.0, COALESCE(behavioral_growth, 0) / 10.0) * 0.2 + -- 20% behavioral growth
        COALESCE(objective_achievement, 0.0) / 20.0      -- 20% objective achievement
    );
    
    RETURN LEAST(5.0, effectiveness_score);
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 8. MATERIALIZED VIEWS FOR REAL-TIME DASHBOARDS
-- =====================================================

-- Real-time classroom analytics dashboard
CREATE MATERIALIZED VIEW classroom_analytics_dashboard AS
SELECT 
    c.id AS classroom_id,
    c.name AS classroom_name,
    c.classroom_code,
    f.first_name || ' ' || f.last_name AS facilitator_name,
    
    -- Current session metrics
    COUNT(DISTINCT ss.id) AS active_students,
    COUNT(DISTINCT ba.id) AS total_interactions_today,
    AVG(ba.behavioral_score) AS avg_behavioral_score_today,
    
    -- Behavioral category breakdown
    COUNT(CASE WHEN ba.behavioral_category = 'empathy' THEN 1 END) AS empathy_interactions,
    COUNT(CASE WHEN ba.behavioral_category = 'confidence' THEN 1 END) AS confidence_interactions,
    COUNT(CASE WHEN ba.behavioral_category = 'communication' THEN 1 END) AS communication_interactions,
    COUNT(CASE WHEN ba.behavioral_category = 'leadership' THEN 1 END) AS leadership_interactions,
    
    -- Recent lesson effectiveness
    (SELECT AVG(average_engagement_score) 
     FROM lesson_effectiveness le 
     WHERE le.classroom_id = c.id 
     AND le.measurement_date >= CURRENT_DATE - INTERVAL '7 days') AS recent_lesson_effectiveness,
    
    -- Growth indicators
    COUNT(CASE WHEN ba.growth_indicator = true THEN 1 END) AS growth_moments_today,
    
    -- Last updated
    CURRENT_TIMESTAMP AS last_updated
    
FROM classrooms c
JOIN facilitators f ON c.facilitator_id = f.id
LEFT JOIN student_sessions ss ON c.id = ss.classroom_id AND ss.is_active = true
LEFT JOIN behavioral_analytics ba ON ss.id = ba.student_session_id 
    AND ba.tracked_at::DATE = CURRENT_DATE
WHERE c.is_active = true AND f.is_active = true
GROUP BY c.id, c.name, c.classroom_code, f.first_name, f.last_name;

-- Index for fast dashboard queries
CREATE UNIQUE INDEX idx_classroom_analytics_dashboard_classroom 
ON classroom_analytics_dashboard (classroom_id);

-- Facilitator overview analytics
CREATE MATERIALIZED VIEW facilitator_overview_analytics AS
SELECT 
    f.id AS facilitator_id,
    f.first_name || ' ' || f.last_name AS facilitator_name,
    f.organization,
    
    -- Classroom summary
    COUNT(DISTINCT c.id) AS total_classrooms,
    COUNT(DISTINCT ss.id) AS total_active_students,
    
    -- This week's metrics
    COUNT(DISTINCT CASE WHEN ba.tracked_at >= date_trunc('week', CURRENT_TIMESTAMP) THEN ba.id END) AS interactions_this_week,
    AVG(CASE WHEN ba.tracked_at >= date_trunc('week', CURRENT_TIMESTAMP) THEN ba.behavioral_score END) AS avg_score_this_week,
    
    -- Behavioral category totals (this week)
    COUNT(CASE WHEN ba.behavioral_category = 'empathy' AND ba.tracked_at >= date_trunc('week', CURRENT_TIMESTAMP) THEN 1 END) AS empathy_total,
    COUNT(CASE WHEN ba.behavioral_category = 'confidence' AND ba.tracked_at >= date_trunc('week', CURRENT_TIMESTAMP) THEN 1 END) AS confidence_total,
    COUNT(CASE WHEN ba.behavioral_category = 'communication' AND ba.tracked_at >= date_trunc('week', CURRENT_TIMESTAMP) THEN 1 END) AS communication_total,
    COUNT(CASE WHEN ba.behavioral_category = 'leadership' AND ba.tracked_at >= date_trunc('week', CURRENT_TIMESTAMP) THEN 1 END) AS leadership_total,
    
    -- Growth and effectiveness
    COUNT(CASE WHEN ba.growth_indicator = true AND ba.tracked_at >= date_trunc('week', CURRENT_TIMESTAMP) THEN 1 END) AS growth_moments_this_week,
    
    -- Recent lesson effectiveness average
    (SELECT AVG(average_engagement_score) 
     FROM lesson_effectiveness le 
     JOIN classrooms c2 ON le.classroom_id = c2.id 
     WHERE c2.facilitator_id = f.id 
     AND le.measurement_date >= CURRENT_DATE - INTERVAL '7 days') AS recent_avg_effectiveness,
    
    CURRENT_TIMESTAMP AS last_updated
    
FROM facilitators f
LEFT JOIN classrooms c ON f.id = c.facilitator_id AND c.is_active = true
LEFT JOIN student_sessions ss ON c.id = ss.classroom_id AND ss.is_active = true
LEFT JOIN behavioral_analytics ba ON ss.id = ba.student_session_id
WHERE f.is_active = true
GROUP BY f.id, f.first_name, f.last_name, f.organization;

-- Index for facilitator overview queries
CREATE UNIQUE INDEX idx_facilitator_overview_analytics_facilitator 
ON facilitator_overview_analytics (facilitator_id);

-- =====================================================
-- 9. INDEXES FOR PERFORMANCE OPTIMIZATION
-- =====================================================

-- Behavioral analytics indexes
CREATE INDEX idx_behavioral_analytics_student_session ON behavioral_analytics(student_session_id);
CREATE INDEX idx_behavioral_analytics_classroom ON behavioral_analytics(classroom_id);
CREATE INDEX idx_behavioral_analytics_lesson ON behavioral_analytics(lesson_id);
CREATE INDEX idx_behavioral_analytics_category ON behavioral_analytics(behavioral_category);
CREATE INDEX idx_behavioral_analytics_tracked_date ON behavioral_analytics(date(tracked_at));
CREATE INDEX idx_behavioral_analytics_hash ON behavioral_analytics(anonymous_student_hash);
CREATE INDEX idx_behavioral_analytics_score ON behavioral_analytics(behavioral_score);
CREATE INDEX idx_behavioral_analytics_growth ON behavioral_analytics(growth_indicator) WHERE growth_indicator = true;

-- Lesson effectiveness indexes
CREATE INDEX idx_lesson_effectiveness_lesson ON lesson_effectiveness(lesson_id);
CREATE INDEX idx_lesson_effectiveness_classroom ON lesson_effectiveness(classroom_id);
CREATE INDEX idx_lesson_effectiveness_date ON lesson_effectiveness(measurement_date);
CREATE INDEX idx_lesson_effectiveness_score ON lesson_effectiveness(average_engagement_score);

-- Time series analytics indexes
CREATE INDEX idx_time_series_timestamp ON time_series_analytics(measurement_timestamp);
CREATE INDEX idx_time_series_bucket ON time_series_analytics(time_bucket);
CREATE INDEX idx_time_series_classroom ON time_series_analytics(classroom_id);
CREATE INDEX idx_time_series_lesson ON time_series_analytics(lesson_id);
CREATE INDEX idx_time_series_category ON time_series_analytics(behavioral_category);

-- Educational impact metrics indexes
CREATE INDEX idx_educational_impact_study_period ON educational_impact_metrics(study_period_start, study_period_end);
CREATE INDEX idx_educational_impact_cohort ON educational_impact_metrics(study_cohort_identifier);

-- Analytics cache indexes
CREATE INDEX idx_analytics_cache_key ON analytics_aggregation_cache(cache_key);
CREATE INDEX idx_analytics_cache_type ON analytics_aggregation_cache(cache_type);
CREATE INDEX idx_analytics_cache_facilitator ON analytics_aggregation_cache(facilitator_id);
CREATE INDEX idx_analytics_cache_classroom ON analytics_aggregation_cache(classroom_id);
CREATE INDEX idx_analytics_cache_status ON analytics_aggregation_cache(cache_status);
CREATE INDEX idx_analytics_cache_expires ON analytics_aggregation_cache(expires_at);
CREATE INDEX idx_analytics_cache_refresh ON analytics_aggregation_cache(next_refresh_at) WHERE auto_refresh_enabled = true;

-- Anonymous hash salts indexes
CREATE INDEX idx_hash_salts_date ON anonymous_hash_salts(salt_date);
CREATE INDEX idx_hash_salts_active ON anonymous_hash_salts(is_active) WHERE is_active = true;

-- =====================================================
-- 10. TRIGGERS FOR AUTOMATED MAINTENANCE
-- =====================================================

-- Updated_at triggers for enhanced analytics tables
CREATE TRIGGER trigger_behavioral_analytics_updated_at
    BEFORE UPDATE ON behavioral_analytics
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_lesson_effectiveness_updated_at
    BEFORE UPDATE ON lesson_effectiveness
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_time_series_analytics_updated_at
    BEFORE UPDATE ON time_series_analytics
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_educational_impact_metrics_updated_at
    BEFORE UPDATE ON educational_impact_metrics
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_analytics_aggregation_cache_updated_at
    BEFORE UPDATE ON analytics_aggregation_cache
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Refresh materialized views trigger
CREATE OR REPLACE FUNCTION refresh_analytics_materialized_views() RETURNS TRIGGER AS $$
BEGIN
    -- Refresh views asynchronously to avoid blocking
    PERFORM pg_notify('refresh_views', 'classroom_analytics_dashboard');
    PERFORM pg_notify('refresh_views', 'facilitator_overview_analytics');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for real-time view updates
CREATE TRIGGER trigger_refresh_analytics_views
    AFTER INSERT OR UPDATE OR DELETE ON behavioral_analytics
    FOR EACH STATEMENT EXECUTE FUNCTION refresh_analytics_materialized_views();

-- =====================================================
-- 11. DATA RETENTION AND CLEANUP PROCEDURES
-- =====================================================

-- COPPA-compliant data retention function (90-day default)
CREATE OR REPLACE FUNCTION execute_coppa_data_retention(
    retention_days INTEGER DEFAULT 90
) RETURNS JSONB AS $$
DECLARE
    retention_date DATE;
    records_archived INTEGER := 0;
    records_deleted INTEGER := 0;
    result JSONB;
BEGIN
    retention_date := CURRENT_DATE - INTERVAL '1 day' * retention_days;
    
    -- Archive old behavioral analytics data
    INSERT INTO behavioral_analytics_archive 
    SELECT * FROM behavioral_analytics 
    WHERE tracked_at::DATE < retention_date;
    GET DIAGNOSTICS records_archived = ROW_COUNT;
    
    -- Delete archived behavioral analytics
    DELETE FROM behavioral_analytics 
    WHERE tracked_at::DATE < retention_date;
    GET DIAGNOSTICS records_deleted = ROW_COUNT;
    
    -- Clean up old hash salts (keep last 7 days)
    DELETE FROM anonymous_hash_salts 
    WHERE salt_date < CURRENT_DATE - INTERVAL '7 days';
    
    -- Expire old analytics cache
    UPDATE analytics_aggregation_cache 
    SET cache_status = 'expired'
    WHERE expires_at < CURRENT_TIMESTAMP;
    
    -- Log retention execution
    INSERT INTO data_retention_log (
        table_name, retention_policy, records_processed, 
        records_archived, records_deleted, execution_date
    ) VALUES (
        'behavioral_analytics', 
        'coppa_' || retention_days || '_day_retention', 
        records_archived + records_deleted,
        records_archived, 
        records_deleted, 
        CURRENT_DATE
    );
    
    result := jsonb_build_object(
        'retention_date', retention_date,
        'records_archived', records_archived,
        'records_deleted', records_deleted,
        'execution_timestamp', CURRENT_TIMESTAMP
    );
    
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- Create archive table for behavioral analytics
CREATE TABLE behavioral_analytics_archive (
    LIKE behavioral_analytics INCLUDING ALL
);

-- =====================================================
-- 12. ROW LEVEL SECURITY POLICIES
-- =====================================================

-- Enable RLS on enhanced analytics tables
ALTER TABLE behavioral_analytics ENABLE ROW LEVEL SECURITY;
ALTER TABLE lesson_effectiveness ENABLE ROW LEVEL SECURITY;
ALTER TABLE time_series_analytics ENABLE ROW LEVEL SECURITY;
ALTER TABLE educational_impact_metrics ENABLE ROW LEVEL SECURITY;
ALTER TABLE analytics_aggregation_cache ENABLE ROW LEVEL SECURITY;

-- Facilitators can only access their classroom data
CREATE POLICY facilitator_behavioral_analytics_access ON behavioral_analytics
    FOR ALL TO application_role
    USING (classroom_id IN (
        SELECT id FROM classrooms 
        WHERE facilitator_id = current_setting('app.current_facilitator_id')::UUID
    ));

CREATE POLICY facilitator_lesson_effectiveness_access ON lesson_effectiveness
    FOR ALL TO application_role
    USING (classroom_id IN (
        SELECT id FROM classrooms 
        WHERE facilitator_id = current_setting('app.current_facilitator_id')::UUID
    ));

CREATE POLICY facilitator_analytics_cache_access ON analytics_aggregation_cache
    FOR ALL TO application_role
    USING (facilitator_id = current_setting('app.current_facilitator_id')::UUID);

-- Research role access to anonymized data only
CREATE POLICY research_time_series_access ON time_series_analytics
    FOR SELECT TO analytics_role
    USING (true); -- Research role can see aggregated data

CREATE POLICY research_impact_metrics_access ON educational_impact_metrics
    FOR SELECT TO analytics_role
    USING (true); -- Research role can see impact metrics

-- =====================================================
-- SCHEMA DEPLOYMENT COMPLETE
-- =====================================================

-- Insert initial salt for anonymous hashing
INSERT INTO anonymous_hash_salts (salt_date, salt_value)
VALUES (CURRENT_DATE, encode(gen_random_bytes(32), 'hex'))
ON CONFLICT (salt_date) DO NOTHING;

-- Create initial cache entries for dashboard performance
INSERT INTO analytics_aggregation_cache (
    cache_key, cache_type, aggregated_data, data_start_date, data_end_date, 
    aggregation_level, expires_at
) VALUES (
    'global_dashboard_summary', 'facilitator_overview', 
    '{"status": "initialized", "message": "Cache will be populated on first data"}',
    CURRENT_DATE, CURRENT_DATE, 'daily',
    CURRENT_TIMESTAMP + INTERVAL '1 hour'
) ON CONFLICT (cache_key) DO NOTHING;

-- Log enhanced analytics schema deployment
INSERT INTO audit_log (table_name, action, new_values, timestamp) VALUES
('enhanced_analytics_schema', 'INSERT', 
 '{"version": "2.0.0", "deployment_date": "2025-07-10", "enhanced_tables": 5, "functions": 4, "views": 2, "coppa_compliant": true}', 
 CURRENT_TIMESTAMP);

-- Success message
SELECT 'Heroes in Waiting Enhanced Analytics Database Schema Successfully Created!' AS status,
       'COPPA Compliant Anonymous Tracking Enabled' AS compliance_status,
       'Real-time Dashboard Performance Optimized' AS performance_status;