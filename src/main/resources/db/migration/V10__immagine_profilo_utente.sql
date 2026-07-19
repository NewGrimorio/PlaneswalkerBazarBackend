-- ============================================================================
-- PlaneswalkerBazar — V10: immagine profilo utente
--
-- Nullable: NULL = nessuna foto caricata, il frontend mostra il default.
-- Il DB conserva SOLO il percorso relativo (/immagini/utenti/<uuid>.<ext>);
-- i byte vivono nella cartella upload (pattern immagini prodotto, V5).
-- Upload id-bound: file e campo aggiornati nella stessa transazione,
-- niente orfani (a differenza del flusso prodotti, l'utente esiste sempre).
-- ============================================================================

ALTER TABLE utente ADD COLUMN immagine_profilo VARCHAR(500);