-- ============================================================================
-- PlaneswalkerBazar — V9: login con email O username
-- Il campo della LoginReq diventa 'identificativo': la '@' discrimina
-- (lo username non puo' contenerla per pattern, l'email deve).
-- ============================================================================
INSERT INTO messaggi_sistema (lang, code, messaggio) VALUES
('IT', 'utente.no.identificativo', 'Email o username obbligatorio');

UPDATE messaggi_sistema
SET messaggio = 'Credenziali errate'
WHERE lang = 'IT' AND code = 'utente.credenziali.errate';