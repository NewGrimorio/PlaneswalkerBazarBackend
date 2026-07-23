-- ============================================================================
-- V13: metodo di spedizione sull'ordine
--
-- Serve ricordare QUALE metodo e' stato scelto, non solo quanto e' costato:
-- l'admin nella coda ordini deve sapere se spedire in 1-2 o in 4-7 giorni.
--
-- Il costo resta in spese_spedizione (gia' presente da V1, finora sempre 0):
-- e' lo SNAPSHOT della tariffa applicata, come i prezzi di riga.
--
-- Gli ordini esistenti diventano STANDARD con spese 0 — che e' la verita'
-- storica: sono stati creati quando la spedizione non veniva addebitata.
-- ============================================================================

ALTER TABLE ordine
    ADD COLUMN tipo_spedizione VARCHAR(10) NOT NULL DEFAULT 'STANDARD';

ALTER TABLE ordine
    ADD CONSTRAINT chk_tipo_spedizione
    CHECK (tipo_spedizione IN ('STANDARD','EXPRESS'));