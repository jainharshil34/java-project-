-- Table for tracking upvotes per user to prevent duplicate votes
CREATE TABLE complaint_upvotes (
    complaint_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (complaint_id, user_id),
    CONSTRAINT fk_upvote_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE,
    CONSTRAINT fk_upvote_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_complaint_upvotes_user ON complaint_upvotes(user_id);
