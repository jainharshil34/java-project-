-- Seed baseline municipal departments
INSERT INTO departments (id, name, code, description, contact_email, contact_phone, created_at, updated_at)
VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Roads & Infrastructure', 'ROADS_INFRA', 'Management and maintenance of municipal roads, bridges, pavements, and public infrastructure.', 'roads@civic.gov', '+1-555-0101', NOW(), NOW()),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'Sanitation', 'SANITATION', 'Solid waste collection, street sweeping, garbage dumps, and public cleanliness.', 'sanitation@civic.gov', '+1-555-0102', NOW(), NOW()),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33', 'Electricity', 'ELECTRICITY', 'Streetlights, electrical grid faults, open transformers, and lighting fixtures.', 'electricity@civic.gov', '+1-555-0103', NOW(), NOW()),
    ('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44', 'Water Supply', 'WATER_SUPPLY', 'Potable water pipelines, sewage lines, leakages, drainage, and water contamination.', 'watersupply@civic.gov', '+1-555-0104', NOW(), NOW()),
    ('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a55', 'Pollution Control', 'POLLUTION_CONTROL', 'Noise pollution, industrial emissions, chemical waste, and environmental violations.', 'pollution@civic.gov', '+1-555-0105', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;
