-- Wipe any data created during manual testing so DataSeeder
-- can repopulate with clean, realistic demo content.
DELETE FROM payments;
DELETE FROM invoice_items;
DELETE FROM invoices;
DELETE FROM clients;
DELETE FROM refresh_tokens;
DELETE FROM users;
