-- ============================================================================
-- PlaneswalkerBazar — V7: regole SKU per prodotti non-SINGLE
--
-- 1. Un prodotto non-SINGLE ha al massimo UNO SKU: la lingua vive
--    nell'anagrafica del prodotto (es. "... (ENG)"), le varianti sono
--    un concetto delle carte singole. Guard nel service (createSku).
-- 2. La lingua di uno SKU e' correggibile SOLO per i non-SINGLE: per le
--    carte identifica la variante (variante nuova = SKU nuovo).
--    Guard nel service (updateSku).
-- ============================================================================
 
INSERT INTO messaggi_sistema (lang, code, messaggio) VALUES
('IT', 'sku.prodotto.gia.presente', 'Questo prodotto ha gia'' le scorte inserite: modificale dalla riga esistente'),
('IT', 'sku.lingua.immutabile',     'La lingua di una carta singola non si modifica: crea una nuova variante');
 