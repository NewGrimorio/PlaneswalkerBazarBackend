-- ============================================================================
-- V12: prezzo_riferimento per-SKU
-- Cardtrader espone prezzi per variante (condizione + lingua); Cardmarket
-- (via Scryfall) resta per-finitura. La sentinella 'NA' marca le righe non
-- specifiche per condizione/lingua (finish-level), stessa convenzione di
-- magazzino_sku. Le righe storiche esistenti (Scryfall) diventano 'NA'/'NA'
-- grazie al DEFAULT, coerente col loro significato finish-level.
-- ============================================================================

ALTER TABLE prezzo_riferimento
    ADD COLUMN condizione VARCHAR(2) NOT NULL DEFAULT 'NA';

ALTER TABLE prezzo_riferimento
    ADD COLUMN lingua VARCHAR(2) NOT NULL DEFAULT 'NA';

-- Stessi valori del chk_condizione di magazzino_sku (scala Cardmarket a 7 gradi)
ALTER TABLE prezzo_riferimento
    ADD CONSTRAINT chk_pr_condizione
    CHECK (condizione IN ('MT','NM','EX','GD','LP','PL','PO','NA'));

-- L'indice ora copre anche condizione/lingua: la query "ultimo prezzo per
-- coordinata SKU" filtra su tutte e cinque le colonne. Il vecchio indice
-- (stampa, fonte, finitura) ne è un prefisso, quindi lo sostituiamo.
DROP INDEX IF EXISTS idx_prezzo_rif;
CREATE INDEX idx_prezzo_rif
    ON prezzo_riferimento(stampa_id, fonte, finitura, condizione, lingua);