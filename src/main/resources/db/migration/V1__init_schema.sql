-- Enable PostGIS extension for spatial data support
CREATE EXTENSION IF NOT EXISTS postgis;

-- 1. Departments table
CREATE TABLE departments (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500),
    contact_email VARCHAR(120),
    contact_phone VARCHAR(30),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 2. Users table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(120) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(30),
    role VARCHAR(30) NOT NULL,
    department_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
);

-- 3. Complaints table
CREATE TABLE complaints (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(30) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    location GEOMETRY(Point, 4326) NOT NULL,
    address VARCHAR(255),
    severity_score INT NOT NULL DEFAULT 0,
    severity_level VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    upvote_count INT NOT NULL DEFAULT 0,
    duplicate_of_id UUID,
    reporter_id UUID NOT NULL,
    department_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_complaints_duplicate FOREIGN KEY (duplicate_of_id) REFERENCES complaints(id) ON DELETE SET NULL,
    CONSTRAINT fk_complaints_reporter FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_complaints_department FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
);

-- 4. Complaint Images table
CREATE TABLE complaint_images (
    id UUID PRIMARY KEY,
    complaint_id UUID NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    file_size BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_complaint_images_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE
);

-- 5. Complaint Status History audit table
CREATE TABLE complaint_status_history (
    id UUID PRIMARY KEY,
    complaint_id UUID NOT NULL,
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_by_user_id UUID NOT NULL,
    remarks TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_status_history_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE,
    CONSTRAINT fk_status_history_user FOREIGN KEY (changed_by_user_id) REFERENCES users(id) ON DELETE CASCADE
);
