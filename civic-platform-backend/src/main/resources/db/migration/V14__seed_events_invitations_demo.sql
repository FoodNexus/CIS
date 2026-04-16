-- =============================================================================
-- Demo seed: event invitations (2 donors, 10 citizens, 3 upcoming events, invites)
-- Password for all test accounts: InviteTest123!  (same BCrypt as V12)
-- =============================================================================

SET @pwd_hash = '$2b$10$YccVfqIl1oczGTrIlmkDRu.9WE68csphqpdW4JF3Uagz7cgJau5pK';

-- Users: 92001-92002 donors, 92003-92012 citizens (CITIZEN)
INSERT IGNORE INTO `user` (
    id, user_name, email, password, is_admin, user_type, badge, points,
    first_name, last_name, association_name, created_at
) VALUES
(92001, 'seed_donor_alpha', 'seed-donor-alpha@test.local', @pwd_hash, 0, 'DONOR', 'NONE', 0,
 'Alpha', 'Donor', 'Demo Civic Association Alpha', NOW()),
(92002, 'seed_donor_beta', 'seed-donor-beta@test.local', @pwd_hash, 0, 'DONOR', 'NONE', 0,
 'Beta', 'Donor', 'Demo Civic Association Beta', NOW()),
(92003, 'seed_citizen_01', 'seed-citizen-01@test.local', @pwd_hash, 0, 'CITIZEN', 'PLATINUM', 40,
 'Chris', 'Citizen', NULL, NOW()),
(92004, 'seed_citizen_02', 'seed-citizen-02@test.local', @pwd_hash, 0, 'CITIZEN', 'GOLD', 30,
 'Dana', 'Citizen', NULL, NOW()),
(92005, 'seed_citizen_03', 'seed-citizen-03@test.local', @pwd_hash, 0, 'CITIZEN', 'SILVER', 20,
 'Evan', 'Citizen', NULL, NOW()),
(92006, 'seed_citizen_04', 'seed-citizen-04@test.local', @pwd_hash, 0, 'CITIZEN', 'BRONZE', 10,
 'Faye', 'Citizen', NULL, NOW()),
(92007, 'seed_citizen_05', 'seed-citizen-05@test.local', @pwd_hash, 0, 'CITIZEN', 'NONE', 0,
 'Gale', 'Citizen', NULL, NOW()),
(92008, 'seed_citizen_06', 'seed-citizen-06@test.local', @pwd_hash, 0, 'CITIZEN', 'GOLD', 30,
 'Harper', 'Citizen', NULL, NOW()),
(92009, 'seed_citizen_07', 'seed-citizen-07@test.local', @pwd_hash, 0, 'CITIZEN', 'SILVER', 20,
 'Iris', 'Citizen', NULL, NOW()),
(92010, 'seed_citizen_08', 'seed-citizen-08@test.local', @pwd_hash, 0, 'CITIZEN', 'BRONZE', 10,
 'Jules', 'Citizen', NULL, NOW()),
(92011, 'seed_citizen_09', 'seed-citizen-09@test.local', @pwd_hash, 0, 'CITIZEN', 'NONE', 0,
 'Kai', 'Citizen', NULL, NOW()),
(92012, 'seed_citizen_10', 'seed-citizen-10@test.local', @pwd_hash, 0, 'CITIZEN', 'SILVER', 20,
 'Lane', 'Citizen', NULL, NOW());

-- One project per donor (scoring / optional UI)
INSERT IGNORE INTO project (
    id, title, description, goal_amount, current_amount, vote_count, status,
    start_date, created_at, created_by_id
) VALUES
(92901,
 'Demo project — Alpha',
 'Demo project for donor Alpha (seed data).',
 50000.00,
 1200.00,
 3,
 'SUBMITTED',
 CURDATE(),
 NOW(),
 92001),
(92902,
 'Demo project — Beta',
 'Demo project for donor Beta (seed data).',
 30000.00,
 800.00,
 2,
 'SUBMITTED',
 CURDATE(),
 NOW(),
 92002);

-- Three upcoming events (ML feed + invitation tests)
INSERT IGNORE INTO `event` (
    id, title, `date`, `type`, `max_capacity`, description, location,
    current_participants, organizer_id, status, created_at
) VALUES
(
    93001,
    'Neighborhood Green Day',
    DATE_ADD(NOW(), INTERVAL 20 DAY),
    'DISTRIBUTION',
    120,
    'Tree planting and community cleanup.',
    'Riverside Park',
    5,
    92001,
    'UPCOMING',
    NOW()
),
(
    93002,
    'Youth Skills Workshop',
    DATE_ADD(NOW(), INTERVAL 35 DAY),
    'FORMATION',
    80,
    'Hands-on workshops for local youth.',
    'Community Center Hall A',
    3,
    92002,
    'UPCOMING',
    NOW()
),
(
    93003,
    'Food Drive Distribution',
    DATE_ADD(NOW(), INTERVAL 10 DAY),
    'DISTRIBUTION',
    200,
    'Weekly food distribution for families in need.',
    'City Square',
    8,
    92001,
    'UPCOMING',
    NOW()
);

-- Event–citizen invitations: 10 citizens per event (30 rows), INVITED + unique tokens
INSERT IGNORE INTO event_citizen_invitations
    (event_id, citizen_id, match_score, status, invited_at, invitation_token)
VALUES
(93001, 92003, 88.5, 'INVITED', NOW(), 'seed93001-92003-11111111-1111-111111111111'),
(93001, 92004, 85.0, 'INVITED', NOW(), 'seed93001-92004-22222222-2222-222222222222'),
(93001, 92005, 82.0, 'INVITED', NOW(), 'seed93001-92005-33333333-3333-333333333333'),
(93001, 92006, 78.0, 'INVITED', NOW(), 'seed93001-92006-44444444-4444-444444444444'),
(93001, 92007, 75.5, 'INVITED', NOW(), 'seed93001-92007-55555555-5555-555555555555'),
(93001, 92008, 72.0, 'INVITED', NOW(), 'seed93001-92008-66666666-6666-666666666666'),
(93001, 92009, 70.0, 'INVITED', NOW(), 'seed93001-92009-77777777-7777-777777777777'),
(93001, 92010, 68.0, 'INVITED', NOW(), 'seed93001-92010-88888888-8888-888888888888'),
(93001, 92011, 65.0, 'INVITED', NOW(), 'seed93001-92011-99999999-9999-999999999999'),
(93001, 92012, 62.0, 'INVITED', NOW(), 'seed93001-92012-aaaaaaaa-aaaa-aaaaaaaaaaaa'),

(93002, 92003, 90.0, 'INVITED', NOW(), 'seed93002-92003-bbbbbbbb-bbbb-bbbbbbbbbbbb'),
(93002, 92004, 86.0, 'INVITED', NOW(), 'seed93002-92004-cccccccc-cccc-cccccccccccc'),
(93002, 92005, 83.0, 'INVITED', NOW(), 'seed93002-92005-dddddddd-dddd-dddddddddddd'),
(93002, 92006, 80.0, 'INVITED', NOW(), 'seed93002-92006-eeeeeeee-eeee-eeeeeeeeeeee'),
(93002, 92007, 77.0, 'INVITED', NOW(), 'seed93002-92007-ffffffff-ffff-ffffffffffff'),
(93002, 92008, 74.0, 'INVITED', NOW(), 'seed93002-92008-00000000-0000-000000000000'),
(93002, 92009, 71.0, 'INVITED', NOW(), 'seed93002-92009-11111111-1111-111111111111'),
(93002, 92010, 69.0, 'INVITED', NOW(), 'seed93002-92010-22222222-2222-222222222222'),
(93002, 92011, 66.0, 'INVITED', NOW(), 'seed93002-92011-33333333-3333-333333333333'),
(93002, 92012, 63.0, 'INVITED', NOW(), 'seed93002-92012-44444444-4444-444444444444'),

(93003, 92003, 87.0, 'INVITED', NOW(), 'seed93003-92003-55555555-5555-555555555555'),
(93003, 92004, 84.0, 'INVITED', NOW(), 'seed93003-92004-66666666-6666-666666666666'),
(93003, 92005, 81.0, 'INVITED', NOW(), 'seed93003-92005-77777777-7777-777777777777'),
(93003, 92006, 79.0, 'INVITED', NOW(), 'seed93003-92006-88888888-8888-888888888888'),
(93003, 92007, 76.0, 'INVITED', NOW(), 'seed93003-92007-99999999-9999-999999999999'),
(93003, 92008, 73.0, 'INVITED', NOW(), 'seed93003-92008-aaaaaaaa-aaaa-aaaaaaaaaaaa'),
(93003, 92009, 70.0, 'INVITED', NOW(), 'seed93003-92009-bbbbbbbb-bbbb-bbbbbbbbbbbb'),
(93003, 92010, 67.0, 'INVITED', NOW(), 'seed93003-92010-cccccccc-cccc-cccccccccccc'),
(93003, 92011, 64.0, 'INVITED', NOW(), 'seed93003-92011-dddddddd-dddd-dddddddddddd'),
(93003, 92012, 61.0, 'INVITED', NOW(), 'seed93003-92012-eeeeeeee-eeee-eeeeeeeeeeee');

-- ML interactions: EVENT views so SVD can learn event preferences
INSERT IGNORE INTO user_interactions (user_id, entity_type, entity_id, action, created_at) VALUES
(92003, 'EVENT', 93001, 'VIEW', NOW() - INTERVAL 1 DAY),
(92004, 'EVENT', 93001, 'VIEW', NOW() - INTERVAL 2 DAY),
(92005, 'EVENT', 93002, 'VIEW', NOW() - INTERVAL 1 DAY),
(92006, 'EVENT', 93003, 'VIEW', NOW() - INTERVAL 3 DAY),
(92007, 'EVENT', 93002, 'VIEW', NOW() - INTERVAL 4 DAY),
(92008, 'EVENT', 93003, 'VIEW', NOW() - INTERVAL 2 DAY);

-- Notifications (in-app bell) — optional; requires table from Hibernate
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    type VARCHAR(32) NOT NULL,
    read_at TIMESTAMP NULL,
    link_url VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE,
    INDEX idx_notifications_user_id (user_id)
);

INSERT IGNORE INTO notifications (user_id, title, body, type, link_url, created_at) VALUES
(92003, 'Event invitation', 'You were invited to "Neighborhood Green Day". Open My invitations to accept or decline.', 'ENGAGEMENT', '/dashboard?tab=invitations', NOW()),
(92004, 'Event invitation', 'You were invited to "Neighborhood Green Day". Open My invitations to accept or decline.', 'ENGAGEMENT', '/dashboard?tab=invitations', NOW());
