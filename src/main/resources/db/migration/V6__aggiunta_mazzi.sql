-- ============================================================================
-- PlaneswalkerBazar — V6: nuovo tipo prodotto MAZZO (precostruiti)
-- ============================================================================
ALTER TABLE prodotto DROP CONSTRAINT chk_tipo_prodotto;
ALTER TABLE prodotto ADD CONSTRAINT chk_tipo_prodotto CHECK (
    tipo_prodotto IN ('SINGLE','BOOSTER','BOOSTER_BOX','SET_LOTTO',
                      'SIGILLATO','ACCESSORIO','MAZZO')
);