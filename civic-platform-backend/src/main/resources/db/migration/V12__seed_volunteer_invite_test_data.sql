-- =============================================================================
-- Dev / QA seed: volunteer invitation matching (E2E-friendly)
-- Run after Flyway migrations. Safe to re-run on empty DB only; uses INSERT IGNORE.
--
-- Login (all accounts use the same password):
--   Password: InviteTest123!
--
-- Donor (create projects, trigger matching):
--   Email: invite-donor@test.local
--
-- Volunteers (receive invites; use any of these to log in and open "My Invitations"):
--   vol-invite-01@test.local ... vol-invite-12@test.local
--
-- IDs 91001-91099 are reserved for this seed block.
-- =============================================================================

-- BCrypt hash for plain text: InviteTest123! (Spring-compatible $2b$)
SET @pwd_hash = '$2b$10$YccVfqIl1oczGTrIlmkDRu.9WE68csphqpdW4JF3Uagz7cgJau5pK';

-- ---------------------------------------------------------------------------
-- Users: 1 donor + 12 volunteers (AMBASSADOR / PARTICIPANT)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO `user` (
    id, user_name, email, password, is_admin, user_type, badge, points,
    first_name, last_name, association_name, created_at
) VALUES
(91001, 'invite_donor', 'invite-donor@test.local', @pwd_hash, 0, 'DONOR', 'NONE', 0,
 'Invite', 'Donor', 'Test Civic Association', NOW()),
(91002, 'vol_invite_01', 'vol-invite-01@test.local', @pwd_hash, 0, 'AMBASSADOR', 'PLATINUM', 40,
 'Alex', 'Volunteer', NULL, NOW()),
(91003, 'vol_invite_02', 'vol-invite-02@test.local', @pwd_hash, 0, 'AMBASSADOR', 'GOLD', 30,
 'Sam', 'Volunteer', NULL, NOW()),
(91004, 'vol_invite_03', 'vol-invite-03@test.local', @pwd_hash, 0, 'AMBASSADOR', 'SILVER', 20,
 'Jordan', 'Volunteer', NULL, NOW()),
(91005, 'vol_invite_04', 'vol-invite-04@test.local', @pwd_hash, 0, 'PARTICIPANT', 'BRONZE', 10,
 'Casey', 'Volunteer', NULL, NOW()),
(91006, 'vol_invite_05', 'vol-invite-05@test.local', @pwd_hash, 0, 'PARTICIPANT', 'NONE', 0,
 'Riley', 'Volunteer', NULL, NOW()),
(91007, 'vol_invite_06', 'vol-invite-06@test.local', @pwd_hash, 0, 'AMBASSADOR', 'GOLD', 30,
 'Morgan', 'Volunteer', NULL, NOW()),
(91008, 'vol_invite_07', 'vol-invite-07@test.local', @pwd_hash, 0, 'PARTICIPANT', 'SILVER', 20,
 'Taylor', 'Volunteer', NULL, NOW()),
(91009, 'vol_invite_08', 'vol-invite-08@test.local', @pwd_hash, 0, 'PARTICIPANT', 'BRONZE', 10,
 'Jamie', 'Volunteer', NULL, NOW()),
(91010, 'vol_invite_09', 'vol-invite-09@test.local', @pwd_hash, 0, 'AMBASSADOR', 'SILVER', 20,
 'Quinn', 'Volunteer', NULL, NOW()),
(91011, 'vol_invite_10', 'vol-invite-10@test.local', @pwd_hash, 0, 'PARTICIPANT', 'GOLD', 30,
 'Avery', 'Volunteer', NULL, NOW()),
(91012, 'vol_invite_11', 'vol-invite-11@test.local', @pwd_hash, 0, 'PARTICIPANT', 'NONE', 0,
 'Skyler', 'Volunteer', NULL, NOW()),
(91013, 'vol_invite_12', 'vol-invite-12@test.local', @pwd_hash, 0, 'PARTICIPANT', 'BRONZE', 10,
 'Reese', 'Volunteer', NULL, NOW());

-- ---------------------------------------------------------------------------
-- Project owned by donor (for funding history + future project creation tests)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO project (
    id, title, description, goal_amount, current_amount, vote_count, status,
    start_date, created_at, created_by_id
) VALUES (
    91001,
    'Seed project for funding history',
    'Used only to populate volunteer funding counts for scoring.',
    10000.00,
    500.00,
    5,
    'SUBMITTED',
    CURDATE(),
    NOW(),
    91001
);

-- ---------------------------------------------------------------------------
-- Completed event organized by donor; volunteers attended (knows-donor bonus)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO `event` (
    id, title, `date`, `type`, `max_capacity`, description, location,
    current_participants, organizer_id, status, created_at
) VALUES (
    91001,
    'Seed community event (completed)',
    NOW() - INTERVAL 30 DAY,
    'DISTRIBUTION',
    200,
    'Seed data for volunteer matching tests.',
    'Test City',
    12,
    91001,
    'COMPLETED',
    NOW() - INTERVAL 30 DAY
);

-- Event participation: COMPLETED + participant COMPLETED (matches scoring query)
INSERT IGNORE INTO event_participant (registered_at, checked_in_at, completed_at, status, event_id, user_id)
VALUES
(NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, 'COMPLETED', 91001, 91002),
(NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, 'COMPLETED', 91001, 91003),
(NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, 'COMPLETED', 91001, 91004),
(NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, 'COMPLETED', 91001, 91005),
(NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, 'COMPLETED', 91001, 91006),
(NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, 'COMPLETED', 91001, 91007),
(NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, 'COMPLETED', 91001, 91008),
(NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, 'COMPLETED', 91001, 91009),
(NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, 'COMPLETED', 91001, 91010),
(NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, 'COMPLETED', 91001, 91011),
(NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, 'COMPLETED', 91001, 91012),
(NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY, 'COMPLETED', 91001, 91013);

-- ---------------------------------------------------------------------------
-- Funding activity (varied counts per user for scoring tiers)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO project_funding (amount, fund_date, payment_method, project_id, user_id) VALUES
(25.00, NOW() - INTERVAL 10 DAY, 'CARD', 91001, 91002),
(15.00, NOW() - INTERVAL 9 DAY, 'CARD', 91001, 91002),
(10.00, NOW() - INTERVAL 8 DAY, 'CARD', 91001, 91002),
(20.00, NOW() - INTERVAL 7 DAY, 'CARD', 91001, 91003),
(15.00, NOW() - INTERVAL 6 DAY, 'CARD', 91001, 91003),
(30.00, NOW() - INTERVAL 5 DAY, 'CARD', 91001, 91004),
(10.00, NOW() - INTERVAL 4 DAY, 'CARD', 91001, 91005),
(5.00,  NOW() - INTERVAL 3 DAY, 'CARD', 91001, 91006),
(40.00, NOW() - INTERVAL 2 DAY, 'CARD', 91001, 91007),
(12.00, NOW() - INTERVAL 1 DAY, 'CARD', 91001, 91008);

-- ---------------------------------------------------------------------------
-- Recent interactions (recency score)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO user_interactions (user_id, entity_type, entity_id, action, created_at) VALUES
(91002, 'PROJECT', 91001, 'VIEW', NOW() - INTERVAL 2 DAY),
(91003, 'PROJECT', 91001, 'VIEW', NOW() - INTERVAL 3 DAY),
(91004, 'PROJECT', 91001, 'LIKE', NOW() - INTERVAL 5 DAY),
(91005, 'PROJECT', 91001, 'VIEW', NOW() - INTERVAL 10 DAY),
(91006, 'PROJECT', 91001, 'VIEW', NOW() - INTERVAL 20 DAY);
