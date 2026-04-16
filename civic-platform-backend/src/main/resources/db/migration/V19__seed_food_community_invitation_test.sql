-- =============================================================================
-- Food / community invitation test seed (additive — new ID range 95000+)
-- Password for all new accounts: InviteTest123!  (same BCrypt as V12/V14/V18)
--
-- Scenarios (see CitizenRateCalculationService + FoodCommunityContext):
--   • High-match Ettadhamon distribution profile: 95001 — address + 3 past DISTRIBUTION
--     with donor 95000, strong location + distributionParticipation.
--   • Cooking / sustainability ambassador: 95002 — AMBASSADOR, ambassador:local,
--     recipe comments on a food post.
--   • Ramadan / solidarity: 95003 — interests + CAMPAIGN interactions on id 97001,
--     testimonial post.
--   • Fridge / weekend volunteer + reliability: 95004 — interests + 9 completed
--     check-ins + 1 NO_SHOW (checkInReliability).
--   • Low-match citizens: 95005–95006, 95010–95011 (weak location, interests,
--     participation, or reliability).
--
-- Solidarity campaign entity_ids (user_interactions only — no campaign row required):
--   97001 Ramadan, 97002 Hiver, 97003 Urgence alimentaire
-- =============================================================================

SET @pwd_hash = '$2b$10$YccVfqIl1oczGTrIlmkDRu.9WE68csphqpdW4JF3Uagz7cgJau5pK';

-- Optional: posts/comments tables may already exist from JPA; keep minimal DDL for Flyway.
CREATE TABLE IF NOT EXISTS `post` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `creator` VARCHAR(255) NOT NULL,
    `content` TEXT,
    `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    `likes_count` INT NOT NULL DEFAULT 0,
    `type` VARCHAR(50) NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `campaign_id` BIGINT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `content` TEXT,
    `created_at` DATETIME(6) NOT NULL,
    `author_id` BIGINT NOT NULL,
    `post_id` BIGINT NOT NULL,
    PRIMARY KEY (`id`)
);

-- ---------------------------------------------------------------------------
-- Donor + citizens (12 citizens)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO `user` (
    id, user_name, email, password, is_admin, user_type, badge, points,
    first_name, last_name, association_name, address, interests, company_name, created_at
) VALUES
(95000, 'food_donor_seed', 'food-donor-seed@test.local', @pwd_hash, 0, 'DONOR', 'NONE', 0,
 'Samir', 'FoodAssoc', 'Association Solidarité Alimentaire', NULL, NULL, NULL, NOW()),

(95001, 'food_ettadhamon_high', 'food-ettadhamon-high@test.local', @pwd_hash, 0, 'CITIZEN', 'PLATINUM', 40,
 'Leila', 'Ettadhamon', NULL, '12 Cité Ettadhamon, Tunis', 'food distribution, community engagement, solidarity, hunger', 'Neighborhood Mutual Aid', NOW()),
(95002, 'food_cook_ambassador', 'food-cook-ambassador@test.local', @pwd_hash, 0, 'AMBASSADOR', 'GOLD', 30,
 'Amine', 'Chef', NULL, '5 Avenue Tunis Centre', 'cooking, sustainability, ambassador:local, food waste, recipe', 'Community Kitchen Lab', NOW()),
(95003, 'food_ramadan_high', 'food-ramadan-high@test.local', @pwd_hash, 0, 'CITIZEN', 'SILVER', 20,
 'Fatma', 'Solidarity', NULL, 'Rue de la Paix, Tunis', 'muslim community, ramadan, solidarity, iftar, testimonials', NULL, NOW()),
(95004, 'food_fridge_volunteer', 'food-fridge-volunteer@test.local', @pwd_hash, 0, 'CITIZEN', 'SILVER', 20,
 'Karim', 'Weekend', NULL, 'Lotissement Les Jardins, Ariana', 'volunteer, weekend availability, community fridge, sustainability', NULL, NOW()),

(95005, 'food_low_remote', 'food-low-remote@test.local', @pwd_hash, 0, 'CITIZEN', 'NONE', 0,
 'Noor', 'Remote', NULL, 'Remote Outpost 404', NULL, NULL, NOW()),
(95006, 'food_low_knitter', 'food-low-knitter@test.local', @pwd_hash, 0, 'CITIZEN', 'NONE', 0,
 'Paul', 'Knits', NULL, 'Alpine Cabin 7', 'knitting, stamps', NULL, NOW()),

(95007, 'food_mid_tunis', 'food-mid-tunis@test.local', @pwd_hash, 0, 'CITIZEN', 'BRONZE', 10,
 'Sara', 'Mid', NULL, 'Avenue Habib Bourguiba, Tunis', 'community, volunteering', NULL, NOW()),
(95008, 'food_mid_green', 'food-mid-green@test.local', @pwd_hash, 0, 'CITIZEN', 'BRONZE', 10,
 'Omar', 'Green', NULL, 'Lac 2, Tunis', 'environment, sustainability', NULL, NOW()),
(95009, 'food_urgence_voter', 'food-urgence-voter@test.local', @pwd_hash, 0, 'CITIZEN', 'BRONZE', 10,
 'Rania', 'Urgence', NULL, 'Ben Arous industrial zone', 'solidarity, hunger', NULL, NOW()),

(95010, 'food_low_reliability', 'food-low-reliability@test.local', @pwd_hash, 0, 'CITIZEN', 'NONE', 0,
 'Hedi', 'Flake', NULL, 'Carthage Byrsa', 'food', NULL, NOW()),
(95011, 'food_far_away', 'food-far-away@test.local', @pwd_hash, 0, 'CITIZEN', 'NONE', 0,
 'Ines', 'Far', NULL, 'Djerba tourist strip 99', NULL, NULL, NOW()),
(95012, 'food_mid_carthage', 'food-mid-carthage@test.local', @pwd_hash, 0, 'CITIZEN', 'SILVER', 20,
 'Youssef', 'Carthage', NULL, 'Sidi Bou Said cliff path', 'culture, tourism', NULL, NOW());

-- ---------------------------------------------------------------------------
-- Past events (history for rates)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO `event` (
    id, title, `date`, `type`, `max_capacity`, description, location,
    current_participants, organizer_id, status, created_at
) VALUES
(95101, 'Food Distribution — Cité Ettadhamon (past round 1)', NOW() - INTERVAL 200 DAY,
 'DISTRIBUTION', 200, 'Weekly dry goods for families. Food security and solidarity.', 'Cité Ettadhamon distribution point',
 40, 95000, 'COMPLETED', NOW() - INTERVAL 200 DAY),
(95102, 'Food Distribution — Cité Ettadhamon (past round 2)', NOW() - INTERVAL 170 DAY,
 'DISTRIBUTION', 200, 'Dry goods and produce. Community food support.', 'Cité Ettadhamon distribution point',
 38, 95000, 'COMPLETED', NOW() - INTERVAL 170 DAY),
(95103, 'Food Distribution — Cité Ettadhamon (past round 3)', NOW() - INTERVAL 140 DAY,
 'DISTRIBUTION', 200, 'Food parcels for residents. Hunger action week.', 'Cité Ettadhamon distribution point',
 42, 95000, 'COMPLETED', NOW() - INTERVAL 140 DAY),

(95104, 'Cooking Workshop — Leftover Recipes (past)', NOW() - INTERVAL 90 DAY,
 'FORMATION', 50, 'Cooking and sustainability — leftover recipes and food waste.', 'Tunis Innovation Hub',
 30, 95000, 'COMPLETED', NOW() - INTERVAL 90 DAY);

INSERT IGNORE INTO `event` (
    id, title, `date`, `type`, `max_capacity`, description, location,
    current_participants, organizer_id, status, created_at
) VALUES
(95120, 'Fridge restocking run (past) #1', NOW() - INTERVAL 60 DAY,
 'DISTRIBUTION', 80, 'Saturday weekend volunteering — community fridge restocking.', 'Ariana community fridge',
 12, 95000, 'COMPLETED', NOW() - INTERVAL 60 DAY),
(95121, 'Fridge restocking run (past) #2', NOW() - INTERVAL 58 DAY,
 'DISTRIBUTION', 80, 'Weekend shift — food rescue to fridges.', 'Ariana community fridge',
 12, 95000, 'COMPLETED', NOW() - INTERVAL 58 DAY),
(95122, 'Fridge restocking run (past) #3', NOW() - INTERVAL 56 DAY,
 'DISTRIBUTION', 80, 'Saturday volunteering — restock community fridge.', 'Ariana community fridge',
 12, 95000, 'COMPLETED', NOW() - INTERVAL 56 DAY),
(95123, 'Fridge restocking run (past) #4', NOW() - INTERVAL 54 DAY,
 'DISTRIBUTION', 80, 'Weekend food solidarity — fridge run.', 'Ariana community fridge',
 12, 95000, 'COMPLETED', NOW() - INTERVAL 54 DAY),
(95124, 'Fridge restocking run (past) #5', NOW() - INTERVAL 52 DAY,
 'DISTRIBUTION', 80, 'Saturday — community fridge restocking.', 'Ariana community fridge',
 12, 95000, 'COMPLETED', NOW() - INTERVAL 52 DAY),
(95125, 'Fridge restocking run (past) #6', NOW() - INTERVAL 50 DAY,
 'DISTRIBUTION', 80, 'Weekend volunteering — fridge route.', 'Ariana community fridge',
 12, 95000, 'COMPLETED', NOW() - INTERVAL 50 DAY),
(95126, 'Fridge restocking run (past) #7', NOW() - INTERVAL 48 DAY,
 'DISTRIBUTION', 80, 'Saturday restock — food waste rescue.', 'Ariana community fridge',
 12, 95000, 'COMPLETED', NOW() - INTERVAL 48 DAY),
(95127, 'Fridge restocking run (past) #8', NOW() - INTERVAL 46 DAY,
 'DISTRIBUTION', 80, 'Weekend — community fridge.', 'Ariana community fridge',
 12, 95000, 'COMPLETED', NOW() - INTERVAL 46 DAY),
(95128, 'Fridge restocking run (past) #9', NOW() - INTERVAL 44 DAY,
 'DISTRIBUTION', 80, 'Saturday volunteering — fridge restocking.', 'Ariana community fridge',
 12, 95000, 'COMPLETED', NOW() - INTERVAL 44 DAY),
(95129, 'Fridge restocking run (past) — no-show', NOW() - INTERVAL 10 DAY,
 'DISTRIBUTION', 80, 'Sunday — community fridge restocking (weekend).', 'Ariana community fridge',
 12, 95000, 'COMPLETED', NOW() - INTERVAL 10 DAY);

-- ---------------------------------------------------------------------------
-- Participation history
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO event_participant (registered_at, checked_in_at, completed_at, status, event_id, user_id)
VALUES
(NOW() - INTERVAL 199 DAY, NOW() - INTERVAL 199 DAY, NOW() - INTERVAL 199 DAY, 'COMPLETED', 95101, 95001),
(NOW() - INTERVAL 169 DAY, NOW() - INTERVAL 169 DAY, NOW() - INTERVAL 169 DAY, 'COMPLETED', 95102, 95001),
(NOW() - INTERVAL 139 DAY, NOW() - INTERVAL 139 DAY, NOW() - INTERVAL 139 DAY, 'COMPLETED', 95103, 95001),

(NOW() - INTERVAL 89 DAY, NOW() - INTERVAL 89 DAY, NOW() - INTERVAL 89 DAY, 'COMPLETED', 95104, 95002),

(NOW() - INTERVAL 59 DAY, NOW() - INTERVAL 59 DAY, NOW() - INTERVAL 59 DAY, 'COMPLETED', 95120, 95004),
(NOW() - INTERVAL 57 DAY, NOW() - INTERVAL 57 DAY, NOW() - INTERVAL 57 DAY, 'COMPLETED', 95121, 95004),
(NOW() - INTERVAL 55 DAY, NOW() - INTERVAL 55 DAY, NOW() - INTERVAL 55 DAY, 'COMPLETED', 95122, 95004),
(NOW() - INTERVAL 53 DAY, NOW() - INTERVAL 53 DAY, NOW() - INTERVAL 53 DAY, 'COMPLETED', 95123, 95004),
(NOW() - INTERVAL 51 DAY, NOW() - INTERVAL 51 DAY, NOW() - INTERVAL 51 DAY, 'COMPLETED', 95124, 95004),
(NOW() - INTERVAL 49 DAY, NOW() - INTERVAL 49 DAY, NOW() - INTERVAL 49 DAY, 'COMPLETED', 95125, 95004),
(NOW() - INTERVAL 47 DAY, NOW() - INTERVAL 47 DAY, NOW() - INTERVAL 47 DAY, 'COMPLETED', 95126, 95004),
(NOW() - INTERVAL 45 DAY, NOW() - INTERVAL 45 DAY, NOW() - INTERVAL 45 DAY, 'COMPLETED', 95127, 95004),
(NOW() - INTERVAL 43 DAY, NOW() - INTERVAL 43 DAY, NOW() - INTERVAL 43 DAY, 'COMPLETED', 95128, 95004),
(NOW() - INTERVAL 9 DAY, NULL, NULL, 'NO_SHOW', 95129, 95004),

(NOW() - INTERVAL 30 DAY, NULL, NULL, 'REGISTERED', 95101, 95010),
(NOW() - INTERVAL 28 DAY, NULL, NULL, 'REGISTERED', 95102, 95010),
(NOW() - INTERVAL 26 DAY, NULL, NULL, 'REGISTERED', 95103, 95010),
(NOW() - INTERVAL 24 DAY, NULL, NULL, 'REGISTERED', 95104, 95010),
(NOW() - INTERVAL 22 DAY, NULL, NULL, 'REGISTERED', 95120, 95010);

-- ---------------------------------------------------------------------------
-- Posts + recipe comment (recipeCommentEngagement)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO post (id, creator, content, status, likes_count, type, created_at, campaign_id) VALUES
(96001, 'food_donor_seed',
 'Leftover recipe ideas — share your best cooking tips for food waste and sustainability in our community kitchen.',
 'ACCEPTED', 0, 'STATUS', NOW() - INTERVAL 30 DAY, NULL),
(96002, 'food_ramadan_high',
 'Testimonial: last Ramadan solidarity campaign changed our neighborhood — grateful for the iftar meals and food support.',
 'ACCEPTED', 0, 'TESTIMONIAL', NOW() - INTERVAL 400 DAY, NULL);

INSERT IGNORE INTO comment (id, content, created_at, author_id, post_id) VALUES
(960010, 'Love these leftover recipes — cooking at home helps reduce food waste!', NOW() - INTERVAL 29 DAY, 95002, 96001);

-- ---------------------------------------------------------------------------
-- Interactions: campaigns (solidarity), events (recency), optional funding
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO user_interactions (user_id, entity_type, entity_id, action, created_at) VALUES
(95003, 'CAMPAIGN', 97001, 'VIEW', NOW() - INTERVAL 400 DAY),
(95003, 'CAMPAIGN', 97001, 'VOTE', NOW() - INTERVAL 399 DAY),
(95003, 'CAMPAIGN', 97002, 'VIEW', NOW() - INTERVAL 300 DAY),
(95009, 'CAMPAIGN', 97003, 'VOTE', NOW() - INTERVAL 120 DAY),
(95001, 'EVENT', 95101, 'VIEW', NOW() - INTERVAL 3 DAY),
(95002, 'EVENT', 95104, 'VIEW', NOW() - INTERVAL 2 DAY),
(95002, 'POST', 96001, 'VIEW', NOW() - INTERVAL 1 DAY);

INSERT IGNORE INTO project_funding (amount, fund_date, payment_method, project_id, user_id) VALUES
(25.00, NOW() - INTERVAL 12 DAY, 'CARD', 92901, 95001),
(10.00, NOW() - INTERVAL 11 DAY, 'CARD', 92901, 95003);
