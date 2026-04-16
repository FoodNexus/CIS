-- =============================================================================
-- SCENARIO SEED (additive only — does not modify existing 92xxx rows)
-- Password for all new accounts: InviteTest123!  (same BCrypt as V12/V14)
--
-- PURPOSE (manual / UI testing):
--   • "Beach Cleanup Weekend" (94101) — citizens 94001–94010: coastal addresses,
--     environment interests, past eco event 94110 with donor 92001, strong location/topic match.
--   • "Coding Workshop" (94102) — citizens 94015–94024: Tech Innovation address,
--     technology interests, developer-style company, past lab 94111 with donor 92002.
--   • Low-match citizens 94011–94014: remote address, no interests, no participation — they rank
--     lower when matching runs (same features, weaker values).
--
-- RATE LOGIC (see Java, not SQL): composite = min(100, rawTotal / 170 * 100). Features include
-- badgeEngagement, pastEventParticipation, communityFunding, roleEngagement, platformRecency,
-- donorCompatibility, locationAlignment, interestTopicAlignment, eventInterestEngagement.
--
-- INVITATION ROWS: Pre-inserted for 94101 and 94102 so the organizer view shows tiers and
-- feature_breakdown_json without re-running matching. Use "Run matching again" to recompute.
-- =============================================================================

SET @pwd_hash = '$2b$10$YccVfqIl1oczGTrIlmkDRu.9WE68csphqpdW4JF3Uagz7cgJau5pK';

-- ---------------------------------------------------------------------------
-- Citizens: beach cluster (94001–94010), low-match (94011–94014), tech cluster (94015–94024)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO `user` (
    id, user_name, email, password, is_admin, user_type, badge, points,
    first_name, last_name, association_name, address, interests, company_name, created_at
) VALUES
(94001, 'beach_citizen_01', 'beach-citizen-01@test.local', @pwd_hash, 0, 'CITIZEN', 'PLATINUM', 40,
 'Maya', 'Coastal', NULL, '15 Coastal Beach Park Road, Sea Town', 'environment, sustainability, beach cleanup', 'Marine Conservation Volunteers', NOW()),
(94002, 'beach_citizen_02', 'beach-citizen-02@test.local', @pwd_hash, 0, 'CITIZEN', 'GOLD', 30,
 'Noah', 'Shoreline', NULL, '8 Coastal Beach Park Road, Sea Town', 'environment, eco volunteering, beach', 'Eco Action Group', NOW()),
(94003, 'beach_citizen_03', 'beach-citizen-03@test.local', @pwd_hash, 0, 'CITIZEN', 'GOLD', 30,
 'Olivia', 'Dunes', NULL, '22 Sand Lane near Coastal Beach Park', 'sustainability, environment, climate', NULL, NOW()),
(94004, 'beach_citizen_04', 'beach-citizen-04@test.local', @pwd_hash, 0, 'CITIZEN', 'SILVER', 20,
 'Pia', 'Harbor', NULL, 'Coastal Beach Park neighborhood', 'beach cleanup, environment', NULL, NOW()),
(94005, 'beach_citizen_05', 'beach-citizen-05@test.local', @pwd_hash, 0, 'CITIZEN', 'SILVER', 20,
 'Quinn', 'Reef', NULL, '101 Ocean View, Coastal Beach Park', 'environment, sustainability', NULL, NOW()),
(94006, 'beach_citizen_06', 'beach-citizen-06@test.local', @pwd_hash, 0, 'CITIZEN', 'BRONZE', 10,
 'Rosa', 'Tide', NULL, '7 Coastal Beach Park Road', 'eco, beach', NULL, NOW()),
(94007, 'beach_citizen_07', 'beach-citizen-07@test.local', @pwd_hash, 0, 'CITIZEN', 'BRONZE', 10,
 'Sam', 'Bay', NULL, 'Near Coastal Beach Park', 'environment', NULL, NOW()),
(94008, 'beach_citizen_08', 'beach-citizen-08@test.local', @pwd_hash, 0, 'CITIZEN', 'SILVER', 20,
 'Tara', 'Surf', NULL, 'Coastal Beach Park area', 'sustainability, beach cleanup', NULL, NOW()),
(94009, 'beach_citizen_09', 'beach-citizen-09@test.local', @pwd_hash, 0, 'CITIZEN', 'BRONZE', 10,
 'Uma', 'Shell', NULL, '12 Coastal Beach Park', 'environment, green living', NULL, NOW()),
(94010, 'beach_citizen_10', 'beach-citizen-10@test.local', @pwd_hash, 0, 'CITIZEN', 'NONE', 0,
 'Vince', 'Salt', NULL, 'Coastal Beach Park Road', 'beach, environment', NULL, NOW()),

(94011, 'low_match_01', 'low-match-01@test.local', @pwd_hash, 0, 'CITIZEN', 'NONE', 0,
 'Wren', 'Remote', NULL, 'Remote Mountain Station 17', NULL, NULL, NOW()),
(94012, 'low_match_02', 'low-match-02@test.local', @pwd_hash, 0, 'CITIZEN', 'NONE', 0,
 'Xara', 'Isolated', NULL, 'Desert Outpost 3', 'knitting', NULL, NOW()),
(94013, 'low_match_03', 'low-match-03@test.local', @pwd_hash, 0, 'CITIZEN', 'NONE', 0,
 'Yuri', 'Faraway', NULL, 'Highland Cabin 99', NULL, NULL, NOW()),
(94014, 'low_match_04', 'low-match-04@test.local', @pwd_hash, 0, 'CITIZEN', 'NONE', 0,
 'Zed', 'Distant', NULL, 'Arctic Research Post 1', 'stamp collecting', NULL, NOW()),

(94015, 'tech_citizen_01', 'tech-citizen-01@test.local', @pwd_hash, 0, 'CITIZEN', 'PLATINUM', 40,
 'Ada', 'Coder', NULL, '2 Tech Innovation Center Drive', 'technology, software development, coding workshops, python', 'Software Developer Studio', NOW()),
(94016, 'tech_citizen_02', 'tech-citizen-02@test.local', @pwd_hash, 0, 'CITIZEN', 'GOLD', 30,
 'Ben', 'Dev', NULL, '5 Tech Innovation Center', 'coding, python, technology', 'Tech Startup Labs', NOW()),
(94017, 'tech_citizen_03', 'tech-citizen-03@test.local', @pwd_hash, 0, 'CITIZEN', 'GOLD', 30,
 'Cara', 'Stack', NULL, 'Tech Innovation Center Suite 200', 'software development, workshop, technology', 'Developer Collective', NOW()),
(94018, 'tech_citizen_04', 'tech-citizen-04@test.local', @pwd_hash, 0, 'CITIZEN', 'SILVER', 20,
 'Dan', 'Git', NULL, '9 Tech Innovation Center Drive', 'technology, coding', 'Software Developer', NOW()),
(94019, 'tech_citizen_05', 'tech-citizen-05@test.local', @pwd_hash, 0, 'CITIZEN', 'SILVER', 20,
 'Eve', 'Branch', NULL, 'Near Tech Innovation Center', 'python, technology, workshops', NULL, NOW()),
(94020, 'tech_citizen_06', 'tech-citizen-06@test.local', @pwd_hash, 0, 'CITIZEN', 'BRONZE', 10,
 'Finn', 'Merge', NULL, '3 Tech Innovation Center', 'coding workshops, developer', 'Junior Developer Inc', NOW()),
(94021, 'tech_citizen_07', 'tech-citizen-07@test.local', @pwd_hash, 0, 'CITIZEN', 'BRONZE', 10,
 'Gia', 'Repo', NULL, 'Tech Innovation Center campus', 'technology, software', NULL, NOW()),
(94022, 'tech_citizen_08', 'tech-citizen-08@test.local', @pwd_hash, 0, 'CITIZEN', 'SILVER', 20,
 'Hal', 'Build', NULL, '11 Tech Innovation Center Drive', 'technology, python', 'Open Source Guild', NOW()),
(94023, 'tech_citizen_09', 'tech-citizen-09@test.local', @pwd_hash, 0, 'CITIZEN', 'BRONZE', 10,
 'Ivy', 'Deploy', NULL, 'Tech Innovation Center', 'coding, workshop', NULL, NOW()),
(94024, 'tech_citizen_10', 'tech-citizen-10@test.local', @pwd_hash, 0, 'CITIZEN', 'NONE', 0,
 'Jake', 'Script', NULL, '6 Tech Innovation Center', 'coding, technology', NULL, NOW());

-- ---------------------------------------------------------------------------
-- Past events (completed history for participation + donor affinity)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO `event` (
    id, title, `date`, `type`, `max_capacity`, description, location,
    current_participants, organizer_id, status, created_at
) VALUES
(
    94110,
    'Eco Shoreline Restoration',
    NOW() - INTERVAL 120 DAY,
    'DISTRIBUTION',
    200,
    'Volunteer day restoring dunes and shoreline habitat. Environment-focused.',
    'Coastal Beach Park',
    24,
    92001,
    'COMPLETED',
    NOW() - INTERVAL 120 DAY
),
(
    94111,
    'Python Study Lab',
    NOW() - INTERVAL 100 DAY,
    'FORMATION',
    60,
    'Hands-on Python exercises for community learners. Technology workshop.',
    'Tech Innovation Center',
    20,
    92002,
    'COMPLETED',
    NOW() - INTERVAL 100 DAY
);

-- ---------------------------------------------------------------------------
-- Upcoming scenario events (invite targets)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO `event` (
    id, title, `date`, `type`, `max_capacity`, description, location,
    current_participants, organizer_id, status, created_at
) VALUES
(
    94101,
    'Beach Cleanup Weekend',
    DATE_ADD(NOW(), INTERVAL 28 DAY),
    'DISTRIBUTION',
    150,
    'Community beach cleanup and recycling. Environment and sustainability focus.',
    'Coastal Beach Park',
    0,
    92001,
    'UPCOMING',
    NOW()
),
(
    94102,
    'Coding Workshop',
    DATE_ADD(NOW(), INTERVAL 40 DAY),
    'FORMATION',
    80,
    'Practical coding and software workshop for local developers. Technology focus.',
    'Tech Innovation Center',
    0,
    92002,
    'UPCOMING',
    NOW()
);

-- ---------------------------------------------------------------------------
-- Completed participation (past eco event: beach cluster; past lab: tech cluster)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO event_participant (registered_at, checked_in_at, completed_at, status, event_id, user_id)
VALUES
(NOW() - INTERVAL 119 DAY, NOW() - INTERVAL 119 DAY, NOW() - INTERVAL 119 DAY, 'COMPLETED', 94110, 94001),
(NOW() - INTERVAL 119 DAY, NOW() - INTERVAL 119 DAY, NOW() - INTERVAL 119 DAY, 'COMPLETED', 94110, 94002),
(NOW() - INTERVAL 119 DAY, NOW() - INTERVAL 119 DAY, NOW() - INTERVAL 119 DAY, 'COMPLETED', 94110, 94003),
(NOW() - INTERVAL 119 DAY, NOW() - INTERVAL 119 DAY, NOW() - INTERVAL 119 DAY, 'COMPLETED', 94110, 94004),
(NOW() - INTERVAL 119 DAY, NOW() - INTERVAL 119 DAY, NOW() - INTERVAL 119 DAY, 'COMPLETED', 94110, 94005),
(NOW() - INTERVAL 119 DAY, NOW() - INTERVAL 119 DAY, NOW() - INTERVAL 119 DAY, 'COMPLETED', 94110, 94006),
(NOW() - INTERVAL 119 DAY, NOW() - INTERVAL 119 DAY, NOW() - INTERVAL 119 DAY, 'COMPLETED', 94110, 94007),
(NOW() - INTERVAL 119 DAY, NOW() - INTERVAL 119 DAY, NOW() - INTERVAL 119 DAY, 'COMPLETED', 94110, 94008),
(NOW() - INTERVAL 119 DAY, NOW() - INTERVAL 119 DAY, NOW() - INTERVAL 119 DAY, 'COMPLETED', 94110, 94009),
(NOW() - INTERVAL 119 DAY, NOW() - INTERVAL 119 DAY, NOW() - INTERVAL 119 DAY, 'COMPLETED', 94110, 94010),

(NOW() - INTERVAL 99 DAY, NOW() - INTERVAL 99 DAY, NOW() - INTERVAL 99 DAY, 'COMPLETED', 94111, 94015),
(NOW() - INTERVAL 99 DAY, NOW() - INTERVAL 99 DAY, NOW() - INTERVAL 99 DAY, 'COMPLETED', 94111, 94016),
(NOW() - INTERVAL 99 DAY, NOW() - INTERVAL 99 DAY, NOW() - INTERVAL 99 DAY, 'COMPLETED', 94111, 94017),
(NOW() - INTERVAL 99 DAY, NOW() - INTERVAL 99 DAY, NOW() - INTERVAL 99 DAY, 'COMPLETED', 94111, 94018),
(NOW() - INTERVAL 99 DAY, NOW() - INTERVAL 99 DAY, NOW() - INTERVAL 99 DAY, 'COMPLETED', 94111, 94019),
(NOW() - INTERVAL 99 DAY, NOW() - INTERVAL 99 DAY, NOW() - INTERVAL 99 DAY, 'COMPLETED', 94111, 94020),
(NOW() - INTERVAL 99 DAY, NOW() - INTERVAL 99 DAY, NOW() - INTERVAL 99 DAY, 'COMPLETED', 94111, 94021),
(NOW() - INTERVAL 99 DAY, NOW() - INTERVAL 99 DAY, NOW() - INTERVAL 99 DAY, 'COMPLETED', 94111, 94022),
(NOW() - INTERVAL 99 DAY, NOW() - INTERVAL 99 DAY, NOW() - INTERVAL 99 DAY, 'COMPLETED', 94111, 94023),
(NOW() - INTERVAL 99 DAY, NOW() - INTERVAL 99 DAY, NOW() - INTERVAL 99 DAY, 'COMPLETED', 94111, 94024);

-- ---------------------------------------------------------------------------
-- Event engagement (VIEW) — boosts eventInterestEngagement in rate
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO user_interactions (user_id, entity_type, entity_id, action, created_at) VALUES
(94001, 'EVENT', 94110, 'VIEW', NOW() - INTERVAL 5 DAY),
(94001, 'EVENT', 94101, 'VIEW', NOW() - INTERVAL 2 DAY),
(94015, 'EVENT', 94111, 'VIEW', NOW() - INTERVAL 4 DAY),
(94015, 'EVENT', 94102, 'VIEW', NOW() - INTERVAL 1 DAY);

-- Funding for a few users (optional tier for communityFunding feature)
INSERT IGNORE INTO project_funding (amount, fund_date, payment_method, project_id, user_id) VALUES
(20.00, NOW() - INTERVAL 20 DAY, 'CARD', 92901, 94001),
(15.00, NOW() - INTERVAL 18 DAY, 'CARD', 92901, 94015);

-- ---------------------------------------------------------------------------
-- Pre-seeded invitations (organizer UI + API) — 10 per scenario event
-- feature_breakdown_json documents features at invite time (see CitizenRateCalculationService)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO event_citizen_invitations
    (event_id, citizen_id, match_score, composite_rate, invitation_tier, priority_followup, status, invited_at, invitation_token, feature_breakdown_json)
VALUES
(94101, 94001, 128.5, 75.6, 'PRIORITY_IMMEDIATE', 1, 'INVITED', NOW(), 'scen94101-94001-aaaaaaaa-aaaa-aaaaaaaaaaaa',
 '{"badgeEngagement":40,"pastEventParticipation":5,"communityFunding":8,"roleEngagement":5,"platformRecency":10,"donorCompatibility":15,"locationAlignment":15,"interestTopicAlignment":18,"eventInterestEngagement":9,"rawTotal":125.0,"compositeRate":73.53}'),
(94101, 94002, 118.0, 69.4, 'STANDARD_INVITE', 0, 'INVITED', NOW(), 'scen94101-94002-bbbbbbbb-bbbb-bbbbbbbbbbbb',
 '{"badgeEngagement":30,"pastEventParticipation":5,"communityFunding":8,"roleEngagement":5,"platformRecency":10,"donorCompatibility":15,"locationAlignment":15,"interestTopicAlignment":18,"eventInterestEngagement":6,"rawTotal":112.0,"compositeRate":65.88}'),
(94101, 94003, 115.0, 67.6, 'STANDARD_INVITE', 0, 'INVITED', NOW(), 'scen94101-94003-cccccccc-cccc-cccccccccccc',
 '{"badgeEngagement":30,"pastEventParticipation":5,"communityFunding":0,"roleEngagement":5,"platformRecency":10,"donorCompatibility":15,"locationAlignment":15,"interestTopicAlignment":18,"eventInterestEngagement":6,"rawTotal":104.0,"compositeRate":61.18}'),
(94101, 94004, 110.0, 64.7, 'STANDARD_INVITE', 0, 'INVITED', NOW(), 'scen94101-94004-dddddddd-dddd-dddddddddddd',
 '{"badgeEngagement":20,"pastEventParticipation":5,"communityFunding":8,"roleEngagement":5,"platformRecency":10,"donorCompatibility":15,"locationAlignment":15,"interestTopicAlignment":18,"eventInterestEngagement":6,"rawTotal":102.0,"compositeRate":60.0}'),
(94101, 94005, 108.0, 63.5, 'STANDARD_INVITE', 0, 'INVITED', NOW(), 'scen94101-94005-eeeeeeee-eeee-eeeeeeeeeeee',
 '{"badgeEngagement":20,"pastEventParticipation":5,"communityFunding":0,"roleEngagement":5,"platformRecency":10,"donorCompatibility":15,"locationAlignment":15,"interestTopicAlignment":18,"eventInterestEngagement":6,"rawTotal":94.0,"compositeRate":55.29}'),
(94101, 94006, 98.0, 57.6, 'STANDARD_INVITE', 0, 'INVITED', NOW(), 'scen94101-94006-ffffffff-ffff-ffffffffffff',
 '{"badgeEngagement":10,"pastEventParticipation":5,"communityFunding":0,"roleEngagement":5,"platformRecency":10,"donorCompatibility":15,"locationAlignment":15,"interestTopicAlignment":18,"eventInterestEngagement":6,"rawTotal":84.0,"compositeRate":49.41}'),
(94101, 94007, 96.0, 56.5, 'STANDARD_INVITE', 0, 'INVITED', NOW(), 'scen94101-94007-00000000-0000-000000000000',
 '{"badgeEngagement":10,"pastEventParticipation":5,"communityFunding":0,"roleEngagement":5,"platformRecency":10,"donorCompatibility":15,"locationAlignment":15,"interestTopicAlignment":18,"eventInterestEngagement":6,"rawTotal":84.0,"compositeRate":49.41}'),
(94101, 94008, 105.0, 61.8, 'STANDARD_INVITE', 0, 'INVITED', NOW(), 'scen94101-94008-11111111-1111-111111111111',
 '{"badgeEngagement":20,"pastEventParticipation":5,"communityFunding":0,"roleEngagement":5,"platformRecency":10,"donorCompatibility":15,"locationAlignment":15,"interestTopicAlignment":18,"eventInterestEngagement":6,"rawTotal":94.0,"compositeRate":55.29}'),
(94101, 94009, 102.0, 60.0, 'STANDARD_INVITE', 0, 'INVITED', NOW(), 'scen94101-94009-22222222-2222-222222222222',
 '{"badgeEngagement":10,"pastEventParticipation":5,"communityFunding":0,"roleEngagement":5,"platformRecency":10,"donorCompatibility":15,"locationAlignment":15,"interestTopicAlignment":18,"eventInterestEngagement":6,"rawTotal":84.0,"compositeRate":49.41}'),
(94101, 94010, 100.0, 58.8, 'STANDARD_INVITE', 0, 'INVITED', NOW(), 'scen94101-94010-33333333-3333-333333333333',
 '{"badgeEngagement":0,"pastEventParticipation":5,"communityFunding":0,"roleEngagement":5,"platformRecency":10,"donorCompatibility":15,"locationAlignment":15,"interestTopicAlignment":18,"eventInterestEngagement":6,"rawTotal":74.0,"compositeRate":43.53}'),

(94102, 94015, 128.0, 75.3, 'PRIORITY_IMMEDIATE', 1, 'INVITED', NOW(), 'scen94102-94015-44444444-4444-444444444444',
 '{"badgeEngagement":40,"pastEventParticipation":5,"communityFunding":8,"roleEngagement":5,"platformRecency":10,"donorCompatibility":15,"locationAlignment":15,"interestTopicAlignment":18,"eventInterestEngagement":9,"rawTotal":125.0,"compositeRate":73.53}'),
(94102, 94016, 118.0, 69.4, 'STANDARD_INVITE', 0, 'INVITED', NOW(), 'scen94102-94016-55555555-5555-555555555555',
 '{"badgeEngagement":30,"pastEventParticipation":5,"communityFunding":8,"roleEngagement":5,"platformRecency":10,"donorCompatibility":15,"locationAlignment":15,"interestTopicAlignment":18,"eventInterestEngagement":6,"rawTotal":112.0,"compositeRate":65.88}'),
(94102, 94017, 115.0, 67.6, 'STANDARD_INVITE', 0, 'INVITED', NOW(), 'scen94102-94017-66666666-6666-666666666666',
 '{"badgeEngagement":30,"pastEventParticipation":5,"communityFunding":0,"roleEngagement":5,"platformRecency":10,"donorCompatibility":15,"locationAlignment":15,"interestTopicAlignment":18,"eventInterestEngagement":6,"rawTotal":104.0,"compositeRate":61.18}'),
(94102, 94018, 110.0, 64.7, 'STANDARD_INVITE', 0, 'INVITED', NOW(), 'scen94102-94018-77777777-7777-777777777777',
 '{"badgeEngagement":20,"pastEventParticipation":5,"communityFunding":8,"roleEngagement":5,"platformRecency":10,"donorCompatibility":15,"locationAlignment":15,"interestTopicAlignment":18,"eventInterestEngagement":6,"rawTotal":102.0,"compositeRate":60.0}'),
(94102, 94019, 108.0, 63.5, 'STANDARD_INVITE', 0, 'INVITED', NOW(), 'scen94102-94019-88888888-8888-888888888888',
 '{"badgeEngagement":20,"pastEventParticipation":5,"communityFunding":0,"roleEngagement":5,"platformRecency":10,"donorCompatibility":15,"locationAlignment":15,"interestTopicAlignment":18,"eventInterestEngagement":6,"rawTotal":94.0,"compositeRate":55.29}'),
(94102, 94020, 98.0, 57.6, 'STANDARD_INVITE', 0, 'INVITED', NOW(), 'scen94102-94020-99999999-9999-999999999999',
 '{"badgeEngagement":10,"pastEventParticipation":5,"communityFunding":0,"roleEngagement":5,"platformRecency":10,"donorCompatibility":15,"locationAlignment":15,"interestTopicAlignment":18,"eventInterestEngagement":6,"rawTotal":84.0,"compositeRate":49.41}'),
(94102, 94021, 96.0, 56.5, 'STANDARD_INVITE', 0, 'INVITED', NOW(), 'scen94102-94021-aaaaaaaa-aaaa-aaaaaaaaaaaa',
 '{"badgeEngagement":10,"pastEventParticipation":5,"communityFunding":0,"roleEngagement":5,"platformRecency":10,"donorCompatibility":15,"locationAlignment":15,"interestTopicAlignment":18,"eventInterestEngagement":6,"rawTotal":84.0,"compositeRate":49.41}'),
(94102, 94022, 105.0, 61.8, 'STANDARD_INVITE', 0, 'INVITED', NOW(), 'scen94102-94022-bbbbbbbb-bbbb-bbbbbbbbbbbb',
 '{"badgeEngagement":20,"pastEventParticipation":5,"communityFunding":0,"roleEngagement":5,"platformRecency":10,"donorCompatibility":15,"locationAlignment":15,"interestTopicAlignment":18,"eventInterestEngagement":6,"rawTotal":94.0,"compositeRate":55.29}'),
(94102, 94023, 102.0, 60.0, 'STANDARD_INVITE', 0, 'INVITED', NOW(), 'scen94102-94023-cccccccc-cccc-cccccccccccc',
 '{"badgeEngagement":10,"pastEventParticipation":5,"communityFunding":0,"roleEngagement":5,"platformRecency":10,"donorCompatibility":15,"locationAlignment":15,"interestTopicAlignment":18,"eventInterestEngagement":6,"rawTotal":84.0,"compositeRate":49.41}'),
(94102, 94024, 100.0, 58.8, 'STANDARD_INVITE', 0, 'INVITED', NOW(), 'scen94102-94024-dddddddd-dddd-dddddddddddd',
 '{"badgeEngagement":0,"pastEventParticipation":5,"communityFunding":0,"roleEngagement":5,"platformRecency":10,"donorCompatibility":15,"locationAlignment":15,"interestTopicAlignment":18,"eventInterestEngagement":6,"rawTotal":74.0,"compositeRate":43.53}');
