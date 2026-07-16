-- ============================================================================
-- PlaneswalkerBazar — V4: ciclo di vita delle stampe (soft-delete + orfani)
--
-- Decisioni:
--   1. attivo: soft-delete deciso SOLO dall'admin, mai automatico durante la
--      sync (stesso pattern di indirizzo e conto_bancario). La disattivazione
--      con giacenza SKU residua e' bloccata nel service (stampa.giacenza.residua).
--   2. orfana: stato di rilevamento scritto SOLO dalla sync, tramite diff
--      in-memory sugli scryfall_id visti nell'import. Stato esplicito, non
--      inferito da update_date: @UpdateTimestamp non tocca le entita' non
--      dirty, quindi il confronto coi timestamp produrrebbe falsi positivi.
--   3. data_ultima_sincronizzazione: alimenta la colonna "Ultima sync" della
--      pagina admin (nome italiano: data di dominio, non di audit).
--
-- Niente indici nuovi: le query di catalogo filtrano attivo insieme a
-- espansione_id o carta_id, gia' coperti da uq_stampa_numero (leading column
-- espansione_id) e idx_stampa_carta. Un indice su un boolean da solo non
-- offre selettivita' utile.
-- ============================================================================

ALTER TABLE stampa ADD COLUMN attivo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE stampa ADD COLUMN orfana BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE espansione ADD COLUMN data_ultima_sincronizzazione TIMESTAMP;

-- ---------------------------------------------------------------------------
-- Messaggi business collegati (V2/V3 congelate: i nuovi codici entrano da qui)
-- ---------------------------------------------------------------------------
INSERT INTO messaggi_sistema (lang, code, messaggio) VALUES
('IT', 'stampa.giacenza.residua', 'Impossibile disattivare: esistono articoli con giacenza residua'),
('IT', 'stampa.gia.disattivata',  'Stampa gia'' disattivata');