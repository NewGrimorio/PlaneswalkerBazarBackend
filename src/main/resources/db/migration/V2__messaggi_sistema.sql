-- ============================================================================
-- PlaneswalkerBazar — V2: popolamento messaggi di sistema (lang = IT)
-- Convenzione codici: minuscolo.puntato (entita'.problema[.dettaglio])
-- NB: una volta applicata in produzione questa V2 e' congelata:
--     i nuovi codici andranno in V3, V4, ...
-- ============================================================================

INSERT INTO messaggi_sistema (lang, code, messaggio) VALUES

-- ---------------------------------------------------------------------------
-- Errori generici (GlobalExceptionHandler)
-- ---------------------------------------------------------------------------
('IT', 'errore.generico',                'Si e'' verificato un errore imprevisto, riprova piu'' tardi'),
('IT', 'errore.validazione',             'Dati inseriti non validi'),

-- ---------------------------------------------------------------------------
-- Validazione UtenteReq / Login / CambioPassword / CambioEmail
-- ---------------------------------------------------------------------------
('IT', 'utente.no.id',                   'Identificativo utente mancante'),
('IT', 'utente.no.email',                'L''email e'' obbligatoria'),
('IT', 'utente.email.invalid',           'Formato email non valido'),
('IT', 'utente.no.pwd',                  'La password e'' obbligatoria'),
('IT', 'utente.pwd.length',              'La password deve avere tra 8 e 72 caratteri'),
('IT', 'utente.no.nome',                 'Il nome e'' obbligatorio'),
('IT', 'utente.nome.maxlength',          'Nome troppo lungo (max 100 caratteri)'),
('IT', 'utente.no.cognome',              'Il cognome e'' obbligatorio'),
('IT', 'utente.cognome.maxlength',       'Cognome troppo lungo (max 100 caratteri)'),
('IT', 'utente.telefono.invalid',        'Numero di telefono non valido'),
('IT', 'utente.nascita.invalid',         'Data di nascita non valida'),
('IT', 'utente.cf.invalid',              'Codice fiscale non valido (16 caratteri alfanumerici)'),

-- ---------------------------------------------------------------------------
-- Validazione IndirizzoReq
-- ---------------------------------------------------------------------------
('IT', 'indirizzo.no.id',                'Identificativo indirizzo mancante'),
('IT', 'indirizzo.no.utente',            'Utente mancante'),
('IT', 'indirizzo.etichetta.maxlength',  'Etichetta troppo lunga (max 50 caratteri)'),
('IT', 'indirizzo.no.destinatario',      'Il destinatario e'' obbligatorio'),
('IT', 'indirizzo.destinatario.maxlength','Destinatario troppo lungo (max 200 caratteri)'),
('IT', 'indirizzo.no.via',               'La via e'' obbligatoria'),
('IT', 'indirizzo.via.maxlength',        'Via troppo lunga (max 200 caratteri)'),
('IT', 'indirizzo.no.civico',            'Il civico e'' obbligatorio'),
('IT', 'indirizzo.civico.maxlength',     'Civico troppo lungo (max 20 caratteri)'),
('IT', 'indirizzo.no.cap',               'Il CAP e'' obbligatorio'),
('IT', 'indirizzo.cap.invalid',          'CAP non valido'),
('IT', 'indirizzo.no.citta',             'La citta'' e'' obbligatoria'),
('IT', 'indirizzo.citta.maxlength',      'Citta'' troppo lunga (max 100 caratteri)'),
('IT', 'indirizzo.provincia.maxlength',  'Provincia troppo lunga (max 50 caratteri)'),
('IT', 'indirizzo.nazione.invalid',      'Codice nazione non valido (2 lettere, es. IT)'),

-- ---------------------------------------------------------------------------
-- Validazione ContoBancarioReq
-- ---------------------------------------------------------------------------
('IT', 'conto.no.utente',                'Utente mancante'),
('IT', 'conto.no.intestatario',          'L''intestatario e'' obbligatorio'),
('IT', 'conto.intestatario.maxlength',   'Intestatario troppo lungo (max 200 caratteri)'),
('IT', 'conto.no.iban',                  'L''IBAN e'' obbligatorio'),
('IT', 'conto.iban.invalid',             'IBAN non valido'),
('IT', 'conto.bic.invalid',              'BIC non valido'),

-- ---------------------------------------------------------------------------
-- Validazione RicaricaReq / PrelievoReq / ConfermaMovimentoReq
-- ---------------------------------------------------------------------------
('IT', 'portafoglio.no.utente',          'Utente mancante'),
('IT', 'portafoglio.no.importo',         'L''importo e'' obbligatorio'),
('IT', 'portafoglio.importo.min',        'L''importo e'' troppo basso'),
('IT', 'portafoglio.importo.invalid',    'Importo non valido (max 2 decimali)'),
('IT', 'portafoglio.no.metodo',          'Il metodo di pagamento e'' obbligatorio'),
('IT', 'portafoglio.no.conto',           'Il conto di destinazione e'' obbligatorio'),
('IT', 'portafoglio.riferimento.maxlength','Riferimento troppo lungo (max 100 caratteri)'),
('IT', 'movimento.no.id',                'Identificativo movimento mancante'),
('IT', 'movimento.no.esito',             'Esito (approva/rifiuta) mancante'),
('IT', 'movimento.nota.maxlength',       'Nota troppo lunga (max 300 caratteri)'),

-- ---------------------------------------------------------------------------
-- Validazione VoceCarrelloReq / CheckoutReq / RecensioneReq
-- ---------------------------------------------------------------------------
('IT', 'carrello.no.utente',             'Utente mancante'),
('IT', 'carrello.no.sku',                'Articolo mancante'),
('IT', 'carrello.no.quantita',           'La quantita'' e'' obbligatoria'),
('IT', 'carrello.quantita.min',          'La quantita'' minima e'' 1'),
('IT', 'ordine.no.utente',               'Utente mancante'),
('IT', 'ordine.no.indirizzo',            'L''indirizzo di consegna e'' obbligatorio'),
('IT', 'recensione.no.utente',           'Utente mancante'),
('IT', 'recensione.no.prodotto',         'Prodotto mancante'),
('IT', 'recensione.no.ordine',           'Ordine di riferimento mancante'),
('IT', 'recensione.no.voto',             'Il voto e'' obbligatorio'),
('IT', 'recensione.voto.range',          'Il voto deve essere tra 1 e 5'),
('IT', 'recensione.titolo.maxlength',    'Titolo troppo lungo (max 150 caratteri)'),
('IT', 'recensione.testo.maxlength',     'Testo troppo lungo (max 4000 caratteri)'),

-- ---------------------------------------------------------------------------
-- Business: PortafoglioImpl (gia' in uso)
-- ---------------------------------------------------------------------------
('IT', 'portafoglio.non.trovato',        'Portafoglio non trovato'),
('IT', 'portafoglio.metodo.non.valido',  'Metodo di ricarica non consentito'),
('IT', 'conto.non.trovato',              'Conto bancario non trovato'),
('IT', 'saldo.insufficiente',            'Saldo del portafoglio insufficiente'),
('IT', 'movimento.non.trovato',          'Movimento non trovato'),
('IT', 'movimento.non.lavorabile',       'Il movimento non e'' piu'' modificabile'),

-- ---------------------------------------------------------------------------
-- Business: service in arrivo (Utente, Indirizzo, Carrello, Ordine, Recensione)
-- ---------------------------------------------------------------------------
('IT', 'utente.non.trovato',             'Utente non trovato'),
('IT', 'utente.credenziali.errate',      'Email o password errati'),
('IT', 'utente.email.duplicata',         'Email gia'' registrata'),
('IT', 'utente.cf.duplicato',            'Codice fiscale gia'' registrato'),
('IT', 'indirizzo.non.trovato',          'Indirizzo non trovato'),
('IT', 'prodotto.non.trovato',           'Prodotto non trovato'),
('IT', 'sku.non.trovato',                'Articolo non trovato'),
('IT', 'sku.non.disponibile',            'Quantita'' richiesta non disponibile'),
('IT', 'carrello.non.trovato',           'Carrello non trovato'),
('IT', 'carrello.vuoto',                 'Il carrello e'' vuoto'),
('IT', 'carrello.voce.non.trovata',      'Articolo non presente nel carrello'),
('IT', 'ordine.non.trovato',             'Ordine non trovato'),
('IT', 'ordine.transizione.non.valida',  'Cambio di stato non consentito'),
('IT', 'recensione.non.consentita',      'Puoi recensire solo prodotti di ordini consegnati'),
('IT', 'recensione.non.trovata',         'Recensione non trovata');