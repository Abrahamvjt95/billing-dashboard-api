-- Re-seed: wipe existing data and let DataSeeder rebuild with enabled=true.
DELETE FROM payments;
DELETE FROM invoice_items;
DELETE FROM invoices;
DELETE FROM clients;
DELETE FROM refresh_tokens;
DELETE FROM users;
