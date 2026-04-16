-- Overlap between user address tokens and event locations improves locationAlignment in rate calculation (demo data).
UPDATE `user` SET address = 'Near Riverside Park, Test City' WHERE id BETWEEN 92003 AND 92006;
UPDATE `user` SET address = 'Community Center area, Test City' WHERE id BETWEEN 92007 AND 92009;
UPDATE `user` SET address = 'Downtown near City Square' WHERE id BETWEEN 92010 AND 92012;
