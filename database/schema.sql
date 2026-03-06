-- =================================================================
-- PLACEMENT INTELLIGENCE SYSTEM - COMPLETE DATABASE SCHEMA
-- Version: 1.0
-- Database: PostgreSQL 16
-- Authors: Shreejal Mehta, Anshumaan Prakash Srivastava, Kartikey Singh
-- Description: Production-level schema for predictive placement
--              readiness and layoff risk intelligence system
-- =================================================================

-- -----------------------------------------------------------------
-- CLEAN SLATE: Drop tables if they exist (for fresh setup)
-- Order matters due to foreign key dependencies
-- -----------------------------------------------------------------
DROP TABLE IF EXISTS readiness_scores CASCADE;
DROP TABLE IF EXISTS student_skills CASCADE;
DROP TABLE IF EXISTS applications CASCADE;
DROP TABLE IF EXISTS risk_signals CASCADE;
DROP TABLE IF EXISTS job_postings CASCADE;
DROP TABLE IF EXISTS company_risk_profiles CASCADE;
DROP TABLE IF EXISTS skills CASCADE;
DROP TABLE IF EXISTS companies CASCADE;
DROP TABLE IF EXISTS departments CASCADE;
DROP TABLE IF EXISTS students CASCADE;

-- =================================================================
-- TABLE 1: DEPARTMENTS
-- Stores academic departments separately (3NF compliance)
-- A student's department is a separate entity, not a string in student
-- =================================================================
CREATE TABLE departments (
    department_id   SERIAL PRIMARY KEY,
    dept_name       VARCHAR(100) NOT NULL UNIQUE,
    dept_code       VARCHAR(10)  NOT NULL UNIQUE,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE departments IS 
'Academic departments. Separated from students table for 3NF compliance.';

-- -----------------------------------------------------------------
-- TABLE 2: STUDENTS
-- Core student profile table
-- graduation_year stored separately from enrollment to avoid redundancy
-- -----------------------------------------------------------------
CREATE TABLE students (
    student_id          SERIAL PRIMARY KEY,
    full_name           VARCHAR(150)        NOT NULL,
    email               VARCHAR(150)        NOT NULL UNIQUE,
    phone               VARCHAR(15)         UNIQUE,
    department_id       INT                 NOT NULL,
    gpa                 DECIMAL(3,2)        NOT NULL CHECK (gpa >= 0.0 AND gpa <= 10.0),
    graduation_year     INT                 NOT NULL CHECK (graduation_year >= 2020 AND graduation_year <= 2035),
    backlogs            INT                 DEFAULT 0 CHECK (backlogs >= 0),
    resume_url          TEXT,
    project_score       DECIMAL(5,2)        DEFAULT 0.0 CHECK (project_score >= 0 AND project_score <= 100),
    is_placed           BOOLEAN             DEFAULT FALSE,
    is_active           BOOLEAN             DEFAULT TRUE,
    created_at          TIMESTAMP           DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP           DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Key
    CONSTRAINT fk_student_department 
        FOREIGN KEY (department_id) 
        REFERENCES departments(department_id) 
        ON DELETE RESTRICT
);

COMMENT ON TABLE students IS 
'Core student profiles. GPA stored as 0-10 scale (Indian system).';
COMMENT ON COLUMN students.project_score IS 
'Manual score out of 100 entered by placement cell based on project quality.';
COMMENT ON COLUMN students.backlogs IS 
'Number of active backlogs. Used in eligibility filtering.';

-- -----------------------------------------------------------------
-- TABLE 3: COMPANIES
-- Master company table with basic info only (3NF)
-- Risk data stored separately in company_risk_profiles
-- -----------------------------------------------------------------
CREATE TABLE companies (
    company_id          SERIAL PRIMARY KEY,
    company_name        VARCHAR(150)        NOT NULL UNIQUE,
    sector              VARCHAR(100)        NOT NULL,
    company_type        VARCHAR(50)         NOT NULL 
                        CHECK (company_type IN ('MNC', 'GCC', 'Startup', 'PSU', 'SME')),
    funding_stage       VARCHAR(50)         
                        CHECK (funding_stage IN ('Bootstrapped', 'Seed', 'Series A', 
                               'Series B', 'Series C', 'Post-IPO', 'Public', 'N/A')),
    headquarters        VARCHAR(100),
    website             VARCHAR(200),
    is_active_recruiter BOOLEAN             DEFAULT TRUE,
    created_at          TIMESTAMP           DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP           DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE companies IS 
'Master company table. Risk data is intentionally separated into 
company_risk_profiles for 3NF compliance and independent updating.';

-- -----------------------------------------------------------------
-- TABLE 4: COMPANY_RISK_PROFILES
-- Separated from companies table (3NF - risk data is a different concern)
-- This table gets updated by triggers when new risk signals arrive
-- -----------------------------------------------------------------
CREATE TABLE company_risk_profiles (
    risk_profile_id         SERIAL PRIMARY KEY,
    company_id              INT             NOT NULL UNIQUE,
    layoff_frequency        DECIMAL(5,2)    DEFAULT 0.0 
                            CHECK (layoff_frequency >= 0 AND layoff_frequency <= 10),
    last_layoff_date        DATE,
    layoff_count_2024       INT             DEFAULT 0,
    layoff_count_2025       INT             DEFAULT 0,
    hiring_trend            VARCHAR(20)     DEFAULT 'stable'
                            CHECK (hiring_trend IN ('growing', 'stable', 'declining', 'frozen')),
    revenue_growth          DECIMAL(6,2)    DEFAULT 0.0,
    automation_impact       VARCHAR(20)     DEFAULT 'medium'
                            CHECK (automation_impact IN ('low', 'medium', 'high', 'critical')),
    stability_score         DECIMAL(5,2)    DEFAULT 50.0 
                            CHECK (stability_score >= 0 AND stability_score <= 100),
    risk_index              DECIMAL(5,2)    DEFAULT 50.0 
                            CHECK (risk_index >= 0 AND risk_index <= 100),
    risk_level              VARCHAR(20)     DEFAULT 'medium'
                            CHECK (risk_level IN ('low', 'medium', 'high', 'critical')),
    last_calculated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    created_at              TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_risk_company 
        FOREIGN KEY (company_id) 
        REFERENCES companies(company_id) 
        ON DELETE CASCADE
);

COMMENT ON TABLE company_risk_profiles IS 
'Stores calculated risk metrics for each company. Automatically updated 
by trigger when new risk_signals are inserted. Risk Index Formula:
Risk Index = (0.4 x Layoff Frequency) - (0.3 x Hiring Growth) - (0.3 x Market Stability)';
COMMENT ON COLUMN company_risk_profiles.risk_index IS 
'0 = safest, 100 = highest risk. Calculated by stored procedure.';

-- -----------------------------------------------------------------
-- TABLE 5: RISK_SIGNALS
-- Individual risk events/news for companies
-- Each signal is a separate data point that influences risk_index
-- -----------------------------------------------------------------
CREATE TABLE risk_signals (
    signal_id       SERIAL PRIMARY KEY,
    company_id      INT             NOT NULL,
    signal_type     VARCHAR(50)     NOT NULL 
                    CHECK (signal_type IN ('LAYOFF_NEWS', 'SEC_FILING', 
                           'FINANCIAL_WARNING', 'HIRING_FREEZE', 
                           'FUNDING_CUT', 'ACQUISITION', 'POSITIVE_NEWS')),
    signal_source   VARCHAR(100),
    headline        TEXT            NOT NULL,
    severity_score  DECIMAL(4,2)    NOT NULL 
                    CHECK (severity_score >= 0 AND severity_score <= 10),
    affected_count  INT             DEFAULT 0,
    is_verified     BOOLEAN         DEFAULT FALSE,
    signal_date     DATE            NOT NULL DEFAULT CURRENT_DATE,
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_signal_company 
        FOREIGN KEY (company_id) 
        REFERENCES companies(company_id) 
        ON DELETE CASCADE
);

COMMENT ON TABLE risk_signals IS 
'Individual risk events. INSERT on this table triggers automatic 
recalculation of company risk_index via trigger.';

-- -----------------------------------------------------------------
-- TABLE 6: SKILLS
-- Master skills catalog (3NF - skills are independent entities)
-- -----------------------------------------------------------------
CREATE TABLE skills (
    skill_id        SERIAL PRIMARY KEY,
    skill_name      VARCHAR(100)    NOT NULL UNIQUE,
    category        VARCHAR(50)     NOT NULL 
                    CHECK (category IN ('Programming', 'Cloud', 'AI/ML', 
                           'Database', 'DevOps', 'System Design', 
                           'Soft Skills', 'Domain')),
    demand_level    VARCHAR(20)     DEFAULT 'medium'
                    CHECK (demand_level IN ('low', 'medium', 'high', 'critical')),
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE skills IS 
'Master catalog of all skills. Normalized separately to avoid 
repeating skill names across student records.';

-- -----------------------------------------------------------------
-- TABLE 7: STUDENT_SKILLS
-- Junction table: Many-to-Many between students and skills
-- -----------------------------------------------------------------
CREATE TABLE student_skills (
    student_skill_id    SERIAL PRIMARY KEY,
    student_id          INT             NOT NULL,
    skill_id            INT             NOT NULL,
    proficiency_level   VARCHAR(20)     NOT NULL 
                        CHECK (proficiency_level IN ('beginner', 'intermediate', 
                               'advanced', 'expert')),
    verified            BOOLEAN         DEFAULT FALSE,
    added_at            TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,

    -- Prevent duplicate skill entries for same student
    CONSTRAINT uq_student_skill UNIQUE (student_id, skill_id),

    CONSTRAINT fk_ss_student 
        FOREIGN KEY (student_id) 
        REFERENCES students(student_id) 
        ON DELETE CASCADE,

    CONSTRAINT fk_ss_skill 
        FOREIGN KEY (skill_id) 
        REFERENCES skills(skill_id) 
        ON DELETE CASCADE
);

COMMENT ON TABLE student_skills IS 
'Junction table for student-skill relationship. Proficiency levels 
allow weighted skill matching in readiness calculation.';

-- -----------------------------------------------------------------
-- TABLE 8: JOB_POSTINGS
-- Job openings posted by companies for campus recruitment
-- -----------------------------------------------------------------
CREATE TABLE job_postings (
    job_id              SERIAL PRIMARY KEY,
    company_id          INT             NOT NULL,
    job_title           VARCHAR(150)    NOT NULL,
    job_description     TEXT,
    required_skills     TEXT            NOT NULL,
    salary_min          DECIMAL(10,2),
    salary_max          DECIMAL(10,2),
    required_gpa        DECIMAL(3,2)    DEFAULT 6.0,
    max_backlogs        INT             DEFAULT 0,
    job_type            VARCHAR(50)     DEFAULT 'Full-Time'
                        CHECK (job_type IN ('Full-Time', 'Internship', 
                               'Contract', 'PPO')),
    experience_required VARCHAR(50)     DEFAULT 'Fresher',
    openings            INT             DEFAULT 1 CHECK (openings > 0),
    application_deadline DATE,
    is_active           BOOLEAN         DEFAULT TRUE,
    posted_at           TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_job_company 
        FOREIGN KEY (company_id) 
        REFERENCES companies(company_id) 
        ON DELETE CASCADE,

    CONSTRAINT chk_salary_range 
        CHECK (salary_max >= salary_min)
);

COMMENT ON TABLE job_postings IS 
'Active job postings. required_skills stored as comma-separated text 
for TF-IDF vectorization in the Java skill matching engine.';

-- -----------------------------------------------------------------
-- TABLE 9: APPLICATIONS
-- Tracks student applications to specific job postings
-- -----------------------------------------------------------------
CREATE TABLE applications (
    application_id      SERIAL PRIMARY KEY,
    student_id          INT             NOT NULL,
    job_id              INT             NOT NULL,
    status              VARCHAR(50)     DEFAULT 'applied'
                        CHECK (status IN ('applied', 'shortlisted', 
                               'interview_scheduled', 'selected', 
                               'rejected', 'offer_revoked', 'withdrawn')),
    applied_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    notes               TEXT,

    -- One student cannot apply to same job twice
    CONSTRAINT uq_application UNIQUE (student_id, job_id),

    CONSTRAINT fk_app_student 
        FOREIGN KEY (student_id) 
        REFERENCES students(student_id) 
        ON DELETE CASCADE,

    CONSTRAINT fk_app_job 
        FOREIGN KEY (job_id) 
        REFERENCES job_postings(job_id) 
        ON DELETE CASCADE
);

-- -----------------------------------------------------------------
-- TABLE 10: READINESS_SCORES
-- Computed readiness scores for student-job combinations
-- This is the core output table of your entire system
-- Formula: (0.5 x skill_match) + (0.2 x gpa_weight) + 
--          (0.2 x project_score) - (0.1 x company_risk)
-- -----------------------------------------------------------------
CREATE TABLE readiness_scores (
    score_id            SERIAL PRIMARY KEY,
    student_id          INT             NOT NULL,
    job_id              INT             NOT NULL,
    skill_match_score   DECIMAL(5,2)    DEFAULT 0.0 
                        CHECK (skill_match_score >= 0 AND skill_match_score <= 100),
    gpa_weight          DECIMAL(5,2)    DEFAULT 0.0 
                        CHECK (gpa_weight >= 0 AND gpa_weight <= 100),
    project_score       DECIMAL(5,2)    DEFAULT 0.0 
                        CHECK (project_score >= 0 AND project_score <= 100),
    company_risk_score  DECIMAL(5,2)    DEFAULT 0.0 
                        CHECK (company_risk_score >= 0 AND company_risk_score <= 100),
    gap_score           DECIMAL(5,2)    DEFAULT 0.0,
    final_readiness     DECIMAL(5,2)    DEFAULT 0.0 
                        CHECK (final_readiness >= 0 AND final_readiness <= 100),
    readiness_level     VARCHAR(20)     DEFAULT 'low'
                        CHECK (readiness_level IN ('low', 'moderate', 
                               'good', 'excellent')),
    missing_skills      TEXT,
    recommendation      TEXT,
    calculated_at       TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,

    -- One readiness score per student-job pair
    CONSTRAINT uq_readiness UNIQUE (student_id, job_id),

    CONSTRAINT fk_rs_student 
        FOREIGN KEY (student_id) 
        REFERENCES students(student_id) 
        ON DELETE CASCADE,

    CONSTRAINT fk_rs_job 
        FOREIGN KEY (job_id) 
        REFERENCES job_postings(job_id) 
        ON DELETE CASCADE
);

COMMENT ON TABLE readiness_scores IS 
'Core output table. Stores computed readiness for every student-job pair.
Final formula: (0.5 x skill_match) + (0.2 x gpa_weight) + 
(0.2 x project_score) - (0.1 x company_risk_score)';

-- =================================================================
-- INDEXES (for query performance)
-- =================================================================

-- B-tree indexes for frequent lookups
CREATE INDEX idx_students_department    ON students(department_id);
CREATE INDEX idx_students_gpa           ON students(gpa DESC);
CREATE INDEX idx_students_graduation    ON students(graduation_year);
CREATE INDEX idx_students_placed        ON students(is_placed);

CREATE INDEX idx_companies_sector       ON companies(sector);
CREATE INDEX idx_companies_type         ON companies(company_type);

CREATE INDEX idx_risk_company           ON company_risk_profiles(company_id);
CREATE INDEX idx_risk_level             ON company_risk_profiles(risk_level);
CREATE INDEX idx_risk_index             ON company_risk_profiles(risk_index ASC);

CREATE INDEX idx_signals_company        ON risk_signals(company_id);
CREATE INDEX idx_signals_date           ON risk_signals(signal_date DESC);
CREATE INDEX idx_signals_type           ON risk_signals(signal_type);

CREATE INDEX idx_jobs_company           ON job_postings(company_id);
CREATE INDEX idx_jobs_active            ON job_postings(is_active);
CREATE INDEX idx_jobs_deadline          ON job_postings(application_deadline);

CREATE INDEX idx_student_skills_student ON student_skills(student_id);
CREATE INDEX idx_student_skills_skill   ON student_skills(skill_id);

CREATE INDEX idx_readiness_student      ON readiness_scores(student_id);
CREATE INDEX idx_readiness_final        ON readiness_scores(final_readiness DESC);

CREATE INDEX idx_applications_student   ON applications(student_id);
CREATE INDEX idx_applications_status    ON applications(status);

COMMENT ON INDEX idx_risk_index IS 
'Ascending index allows fast retrieval of safest companies first.';

-- =================================================================
-- TRIGGER FUNCTIONS
-- =================================================================

-- -----------------------------------------------------------------
-- TRIGGER FUNCTION 1: Auto-update company risk index
-- Fires after every new risk signal is inserted
-- Recalculates risk_index using weighted formula
-- -----------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_company_risk_index()
RETURNS TRIGGER AS $$
DECLARE
    v_avg_severity      DECIMAL(5,2);
    v_signal_count      INT;
    v_layoff_freq       DECIMAL(5,2);
    v_new_risk_index    DECIMAL(5,2);
    v_new_risk_level    VARCHAR(20);
    v_new_stability     DECIMAL(5,2);
BEGIN
    -- Calculate average severity of all signals for this company
    SELECT 
        AVG(severity_score),
        COUNT(*)
    INTO v_avg_severity, v_signal_count
    FROM risk_signals
    WHERE company_id = NEW.company_id;

    -- Calculate layoff frequency (signals per year approximation)
    SELECT layoff_frequency 
    INTO v_layoff_freq
    FROM company_risk_profiles 
    WHERE company_id = NEW.company_id;

    -- Risk Index Formula:
    -- Base: average severity weighted by signal count
    -- Higher severity + more signals = higher risk
    v_new_risk_index := LEAST(100, 
        (v_avg_severity * 10) * (1 + (v_signal_count * 0.05))
    );

    -- Stability score is inverse of risk
    v_new_stability := 100 - v_new_risk_index;

    -- Determine risk level category
    v_new_risk_level := CASE
        WHEN v_new_risk_index >= 75 THEN 'critical'
        WHEN v_new_risk_index >= 50 THEN 'high'
        WHEN v_new_risk_index >= 25 THEN 'medium'
        ELSE 'low'
    END;

    -- Update company risk profile
    UPDATE company_risk_profiles
    SET 
        risk_index          = v_new_risk_index,
        stability_score     = v_new_stability,
        risk_level          = v_new_risk_level,
        last_calculated_at  = CURRENT_TIMESTAMP
    WHERE company_id = NEW.company_id;

    -- Log the update
    RAISE NOTICE 'Risk index updated for company_id %. New risk: %, Level: %', 
        NEW.company_id, v_new_risk_index, v_new_risk_level;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Attach trigger to risk_signals table
CREATE TRIGGER trg_update_risk_on_signal
    AFTER INSERT ON risk_signals
    FOR EACH ROW
    EXECUTE FUNCTION update_company_risk_index();

COMMENT ON FUNCTION update_company_risk_index() IS 
'Automatically recalculates company risk_index whenever a new 
risk signal is inserted. This ensures risk scores are always current.';

-- -----------------------------------------------------------------
-- TRIGGER FUNCTION 2: Auto-update student updated_at timestamp
-- -----------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_student_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_student_updated_at
    BEFORE UPDATE ON students
    FOR EACH ROW
    EXECUTE FUNCTION update_student_timestamp();

-- -----------------------------------------------------------------
-- TRIGGER FUNCTION 3: Auto-update readiness level label
-- Fires after readiness score is calculated
-- -----------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_readiness_level()
RETURNS TRIGGER AS $$
BEGIN
    NEW.readiness_level := CASE
        WHEN NEW.final_readiness >= 80 THEN 'excellent'
        WHEN NEW.final_readiness >= 60 THEN 'good'
        WHEN NEW.final_readiness >= 40 THEN 'moderate'
        ELSE 'low'
    END;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_readiness_level
    BEFORE INSERT OR UPDATE ON readiness_scores
    FOR EACH ROW
    EXECUTE FUNCTION update_readiness_level();

COMMENT ON FUNCTION update_readiness_level() IS 
'Automatically sets readiness_level label based on final_readiness score.
Excellent: 80+, Good: 60-79, Moderate: 40-59, Low: below 40';

-- =================================================================
-- STORED PROCEDURES
-- =================================================================

-- -----------------------------------------------------------------
-- PROCEDURE 1: Calculate Readiness Score
-- Main intelligence procedure of the entire system
-- Called by Java backend via JDBC
-- -----------------------------------------------------------------
CREATE OR REPLACE FUNCTION calculate_readiness(
    p_student_id    INT,
    p_job_id        INT
)
RETURNS TABLE (
    final_score         DECIMAL,
    skill_match         DECIMAL,
    gpa_component       DECIMAL,
    project_component   DECIMAL,
    risk_penalty        DECIMAL,
    missing_skills      TEXT,
    recommendation      TEXT
) AS $$
DECLARE
    v_student_gpa           DECIMAL(3,2);
    v_student_project       DECIMAL(5,2);
    v_company_risk          DECIMAL(5,2);
    v_required_skills       TEXT;
    v_student_skill_count   INT;
    v_required_skill_count  INT;
    v_matched_skills        INT;
    v_skill_match_score     DECIMAL(5,2);
    v_gpa_weight            DECIMAL(5,2);
    v_project_weight        DECIMAL(5,2);
    v_final_readiness       DECIMAL(5,2);
    v_missing_skills        TEXT;
    v_recommendation        TEXT;
    v_company_id            INT;
BEGIN
    -- Fetch student data
    SELECT gpa, project_score 
    INTO v_student_gpa, v_student_project
    FROM students 
    WHERE student_id = p_student_id;

    -- Fetch job's company and required skills
    SELECT company_id, required_skills 
    INTO v_company_id, v_required_skills
    FROM job_postings 
    WHERE job_id = p_job_id;

    -- Fetch company risk index
    SELECT COALESCE(risk_index, 50)
    INTO v_company_risk
    FROM company_risk_profiles 
    WHERE company_id = v_company_id;

    -- Count student skills
    SELECT COUNT(*) 
    INTO v_student_skill_count
    FROM student_skills 
    WHERE student_id = p_student_id;

    -- Simple skill match: count student skills that appear 
    -- in job required_skills text (basic version)
    SELECT COUNT(*)
    INTO v_matched_skills
    FROM student_skills ss
    JOIN skills sk ON ss.skill_id = sk.skill_id
    WHERE ss.student_id = p_student_id
    AND LOWER(v_required_skills) LIKE '%' || LOWER(sk.skill_name) || '%';

    -- Calculate required skill count (approximate from text)
    v_required_skill_count := GREATEST(1, 
        array_length(string_to_array(v_required_skills, ','), 1)
    );

    -- Skill match percentage (0-100)
    v_skill_match_score := LEAST(100,
        (v_matched_skills::DECIMAL / v_required_skill_count) * 100
    );

    -- GPA weight (0-100): normalize GPA to percentage
    -- GPA 10 = 100, GPA 6 = 60, etc.
    v_gpa_weight := (v_student_gpa / 10.0) * 100;

    -- Project score already 0-100
    v_project_weight := COALESCE(v_student_project, 0);

    -- MAIN READINESS FORMULA:
    -- (0.5 x skill_match) + (0.2 x gpa) + (0.2 x project) - (0.1 x risk)
    v_final_readiness := 
        (0.5 * v_skill_match_score) +
        (0.2 * v_gpa_weight) +
        (0.2 * v_project_weight) -
        (0.1 * v_company_risk);

    -- Ensure score stays between 0 and 100
    v_final_readiness := GREATEST(0, LEAST(100, v_final_readiness));

    -- Build missing skills text
    SELECT STRING_AGG(sk.skill_name, ', ')
    INTO v_missing_skills
    FROM skills sk
    WHERE LOWER(v_required_skills) LIKE '%' || LOWER(sk.skill_name) || '%'
    AND sk.skill_id NOT IN (
        SELECT skill_id FROM student_skills 
        WHERE student_id = p_student_id
    );

    -- Generate recommendation
    v_recommendation := CASE
        WHEN v_final_readiness >= 80 THEN 
            'Highly recommended to apply. Strong match.'
        WHEN v_final_readiness >= 60 THEN 
            'Good candidate. Focus on missing skills before applying.'
        WHEN v_final_readiness >= 40 THEN 
            'Moderate readiness. Significant skill gaps need attention.'
        ELSE 
            'Not ready yet. Upskill significantly before applying.'
    END;

    -- Save or update the score in readiness_scores table
    INSERT INTO readiness_scores (
        student_id, job_id, skill_match_score, gpa_weight,
        project_score, company_risk_score, final_readiness,
        missing_skills, recommendation
    ) VALUES (
        p_student_id, p_job_id, v_skill_match_score, v_gpa_weight,
        v_project_weight, v_company_risk, v_final_readiness,
        v_missing_skills, v_recommendation
    )
    ON CONFLICT (student_id, job_id) 
    DO UPDATE SET
        skill_match_score   = EXCLUDED.skill_match_score,
        gpa_weight          = EXCLUDED.gpa_weight,
        project_score       = EXCLUDED.project_score,
        company_risk_score  = EXCLUDED.company_risk_score,
        final_readiness     = EXCLUDED.final_readiness,
        missing_skills      = EXCLUDED.missing_skills,
        recommendation      = EXCLUDED.recommendation,
        calculated_at       = CURRENT_TIMESTAMP;

    -- Return results
    RETURN QUERY SELECT 
        v_final_readiness,
        v_skill_match_score,
        v_gpa_weight,
        v_project_weight,
        v_company_risk,
        v_missing_skills,
        v_recommendation;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION calculate_readiness(INT, INT) IS 
'Main readiness calculation procedure. Takes student_id and job_id,
computes full readiness score using the formula:
(0.5 x skill_match) + (0.2 x gpa_weight) + 
(0.2 x project_score) - (0.1 x company_risk)
Saves result to readiness_scores and returns breakdown.';

-- -----------------------------------------------------------------
-- PROCEDURE 2: Calculate Company Layoff Risk
-- -----------------------------------------------------------------
CREATE OR REPLACE FUNCTION calculate_layoff_risk(
    p_company_id INT
)
RETURNS TABLE (
    company_name        TEXT,
    risk_index          DECIMAL,
    risk_level          TEXT,
    stability_score     DECIMAL,
    total_signals       BIGINT,
    recommendation      TEXT
) AS $$
DECLARE
    v_company_name      VARCHAR(150);
    v_signal_count      BIGINT;
    v_avg_severity      DECIMAL(5,2);
    v_layoff_freq       DECIMAL(5,2);
    v_hiring_trend      VARCHAR(20);
    v_automation        VARCHAR(20);
    v_risk_index        DECIMAL(5,2);
    v_stability         DECIMAL(5,2);
    v_risk_level        VARCHAR(20);
    v_recommendation    TEXT;
    v_automation_score  DECIMAL(5,2);
    v_hiring_score      DECIMAL(5,2);
BEGIN
    -- Get company name
    SELECT company_name INTO v_company_name
    FROM companies WHERE company_id = p_company_id;

    -- Get risk profile data
    SELECT 
        layoff_frequency,
        hiring_trend,
        automation_impact
    INTO v_layoff_freq, v_hiring_trend, v_automation
    FROM company_risk_profiles
    WHERE company_id = p_company_id;

    -- Count and average risk signals
    SELECT COUNT(*), COALESCE(AVG(severity_score), 0)
    INTO v_signal_count, v_avg_severity
    FROM risk_signals
    WHERE company_id = p_company_id;

    -- Convert hiring trend to numeric score
    v_hiring_score := CASE v_hiring_trend
        WHEN 'growing'   THEN 0
        WHEN 'stable'    THEN 25
        WHEN 'declining' THEN 75
        WHEN 'frozen'    THEN 100
        ELSE 50
    END;

    -- Convert automation impact to numeric score
    v_automation_score := CASE v_automation
        WHEN 'low'      THEN 0
        WHEN 'medium'   THEN 33
        WHEN 'high'     THEN 66
        WHEN 'critical' THEN 100
        ELSE 33
    END;

    -- RISK INDEX FORMULA:
    -- Risk Index = (0.4 x Layoff_Freq) + (0.3 x Hiring_Score) + (0.3 x Automation)
    v_risk_index := 
        (0.4 * LEAST(100, v_layoff_freq * 10)) +
        (0.3 * v_hiring_score) +
        (0.3 * v_automation_score);

    v_stability := 100 - v_risk_index;

    -- Risk level categorization
    v_risk_level := CASE
        WHEN v_risk_index >= 75 THEN 'critical'
        WHEN v_risk_index >= 50 THEN 'high'
        WHEN v_risk_index >= 25 THEN 'medium'
        ELSE 'low'
    END;

    -- Recommendation for students
    v_recommendation := CASE
        WHEN v_risk_index >= 75 THEN 
            'HIGH RISK: Avoid applying. Company shows strong instability signals.'
        WHEN v_risk_index >= 50 THEN 
            'CAUTION: Apply carefully. Have backup options ready.'
        WHEN v_risk_index >= 25 THEN 
            'MODERATE: Reasonably safe. Monitor for new developments.'
        ELSE 
            'SAFE: Company shows strong stability. Good choice.'
    END;

    -- Update the stored risk profile
    UPDATE company_risk_profiles
    SET risk_index          = v_risk_index,
        stability_score     = v_stability,
        risk_level          = v_risk_level,
        last_calculated_at  = CURRENT_TIMESTAMP
    WHERE company_id = p_company_id;

    RETURN QUERY SELECT
        v_company_name::TEXT,
        v_risk_index,
        v_risk_level::TEXT,
        v_stability,
        v_signal_count,
        v_recommendation::TEXT;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION calculate_layoff_risk(INT) IS 
'Calculates company layoff risk using formula:
Risk = (0.4 x Layoff_Frequency) + (0.3 x Hiring_Trend) + (0.3 x Automation_Impact)
Returns full risk breakdown and student recommendation.';

-- =================================================================
-- VERIFICATION: Print success message
-- =================================================================
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE 'SCHEMA CREATED SUCCESSFULLY';
    RAISE NOTICE 'Tables: departments, students, companies,';
    RAISE NOTICE '        company_risk_profiles, risk_signals,';
    RAISE NOTICE '        skills, student_skills, job_postings,';
    RAISE NOTICE '        applications, readiness_scores';
    RAISE NOTICE 'Triggers: 3 active triggers';
    RAISE NOTICE 'Functions: 4 stored procedures';
    RAISE NOTICE 'Indexes: 13 performance indexes';
    RAISE NOTICE '========================================';
END $$;