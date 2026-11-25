-- 1. PostGIS Spatial GIST index on complaint location coordinates
CREATE INDEX idx_complaints_location ON complaints USING GIST (location);

-- 2. Performance indexes on high-cardinality filters
CREATE INDEX idx_complaints_category ON complaints (category);
CREATE INDEX idx_complaints_status ON complaints (status);
CREATE INDEX idx_complaints_created_at ON complaints (created_at DESC);
CREATE INDEX idx_complaints_severity_score ON complaints (severity_score DESC);

-- 3. Foreign key relationship indexes
CREATE INDEX idx_complaints_reporter ON complaints (reporter_id);
CREATE INDEX idx_complaints_department ON complaints (department_id);
CREATE INDEX idx_complaints_duplicate_of ON complaints (duplicate_of_id);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_department ON users (department_id);

CREATE INDEX idx_complaint_images_complaint ON complaint_images (complaint_id);
CREATE INDEX idx_status_history_complaint ON complaint_status_history (complaint_id);
