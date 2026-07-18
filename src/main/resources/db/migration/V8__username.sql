-- ============================================================================
-- PlaneswalkerBazar — V8: username utente
--
-- Unicita' case-insensitive garantita dall'applicazione (normalizzazione
-- trim+lowercase in UtenteImpl.normalizzaUsername, unica fonte di verita',
-- stesso pattern dell'email) + UNIQUE semplice. Niente CITEXT ne' indici
-- funzionali: vietati per compatibilita' H2.
--
-- Backfill: localpart dell'email + '.' + id -> leggibile E unico per
-- costruzione (es. alice.1). SUBSTRING/POSITION e || sono portabili
-- PostgreSQL/H2.
-- ============================================================================
 
ALTER TABLE utente ADD COLUMN username VARCHAR(30);
 
UPDATE utente
SET username = LOWER(SUBSTRING(email FROM 1 FOR POSITION('@' IN email) - 1))
               || '.' || id;
 
ALTER TABLE utente ALTER COLUMN username SET NOT NULL;
ALTER TABLE utente ADD CONSTRAINT uq_utente_username UNIQUE (username);
 
INSERT INTO messaggi_sistema (lang, code, messaggio) VALUES
('IT', 'utente.username.duplicato', 'Username gia'' in uso'),
('IT', 'utente.no.username',        'Username obbligatorio'),
('IT', 'utente.username.invalid',   'Username non valido: 3-30 caratteri tra lettere, numeri, punto, trattino e underscore');