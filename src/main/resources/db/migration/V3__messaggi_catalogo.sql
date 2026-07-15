-- ============================================================================
-- PlaneswalkerBazar — V3: messaggi area catalogo (lang = IT)
-- La V2 e' congelata: i nuovi codici entrano da qui (meccanismo Flyway).
-- Copertura: validazione request.products (22 codici, estratti dal codice)
--            + business service catalogo e sync Scryfall (8 codici).
-- ============================================================================
 
INSERT INTO messaggi_sistema (lang, code, messaggio) VALUES
 
-- ---------------------------------------------------------------------------
-- Validazione EspansioneReq
-- ---------------------------------------------------------------------------
('IT', 'espansione.no.id',               'Identificativo espansione mancante'),
('IT', 'espansione.no.codice',           'Il codice del set e'' obbligatorio'),
('IT', 'espansione.codice.maxlength',    'Codice set troppo lungo (max 10 caratteri)'),
('IT', 'espansione.no.nome',             'Il nome del set e'' obbligatorio'),
('IT', 'espansione.nome.maxlength',      'Nome set troppo lungo (max 200 caratteri)'),
('IT', 'espansione.no.tipo',             'Il tipo del set e'' obbligatorio'),
('IT', 'espansione.tipo.maxlength',      'Tipo set troppo lungo (max 30 caratteri)'),
('IT', 'espansione.url.maxlength',       'URL troppo lungo (max 500 caratteri)'),
 
-- ---------------------------------------------------------------------------
-- Validazione ProdottoReq
-- ---------------------------------------------------------------------------
('IT', 'prodotto.no.id',                 'Identificativo prodotto mancante'),
('IT', 'prodotto.no.tipo',               'Il tipo di prodotto e'' obbligatorio'),
('IT', 'prodotto.no.nome',               'Il nome del prodotto e'' obbligatorio'),
('IT', 'prodotto.nome.maxlength',        'Nome prodotto troppo lungo (max 300 caratteri)'),
('IT', 'prodotto.slug.maxlength',        'Slug troppo lungo (max 300 caratteri)'),
('IT', 'prodotto.url.maxlength',         'URL troppo lungo (max 500 caratteri)'),
 
-- ---------------------------------------------------------------------------
-- Validazione MagazzinoSKUReq
-- ---------------------------------------------------------------------------
('IT', 'sku.no.id',                      'Identificativo articolo mancante'),
('IT', 'sku.no.prodotto',                'Il prodotto e'' obbligatorio'),
('IT', 'sku.lingua.invalid',             'Codice lingua non valido (2 lettere, es. en)'),
('IT', 'sku.no.prezzo',                  'Il prezzo e'' obbligatorio'),
('IT', 'sku.prezzo.ngt',                 'Il prezzo non puo'' essere negativo'),
('IT', 'sku.prezzo.invalid',             'Prezzo non valido (max 2 decimali)'),
('IT', 'sku.no.quantita',                'La quantita'' e'' obbligatoria'),
('IT', 'sku.quantita.ngt',               'La quantita'' non puo'' essere negativa'),
 
-- ---------------------------------------------------------------------------
-- Business: service catalogo (in arrivo)
-- ---------------------------------------------------------------------------
('IT', 'espansione.non.trovata',         'Espansione non trovata'),
('IT', 'espansione.codice.duplicato',    'Codice set gia'' esistente'),
('IT', 'prodotto.slug.duplicato',        'Slug gia'' esistente'),
('IT', 'prodotto.tipo.non.modificabile', 'Il tipo di prodotto non e'' modificabile'),
('IT', 'sku.variante.duplicata',         'Variante gia'' esistente per questo prodotto'),
('IT', 'stampa.non.trovata',             'Stampa non trovata'),
 
-- ---------------------------------------------------------------------------
-- Business: sync Scryfall (in arrivo)
-- ---------------------------------------------------------------------------
('IT', 'sync.set.non.trovato',           'Set non trovato su Scryfall'),
('IT', 'sync.errore.comunicazione',      'Errore di comunicazione con Scryfall');