-- ============================================================================
-- PlaneswalkerBazar — Fase 3 · Schema PORTABILE PostgreSQL / H2
-- Target: PostgreSQL 16+ (produzione) e H2 2.x in MODE=PostgreSQL (test)
--
-- Principi di portabilità adottati:
--   * niente tipi ENUM nativi            -> VARCHAR + CHECK (@Enumerated STRING)
--   * niente CITEXT                      -> email normalizzata lowercase nel service
--   * niente array Postgres             -> colonne piatte (colori VARCHAR, boolean finiture)
--   * niente JSONB                       -> TEXT con JSON serializzato (Jackson)
--   * niente indici parziali             -> FK indirizzo_predefinito_id su utente
--   * niente trigger plpgsql             -> @UpdateTimestamp / @PreUpdate in JPA
--   * niente estensioni (pg_trgm, ...)   -> ricerca LIKE, upgrade futuro via migration
--
-- URL H2 consigliato per i test:
--   jdbc:h2:mem:dbJPA;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
-- ============================================================================

-- ============================================================================
-- AREA SISTEMA
-- ============================================================================

-- Messaggi di sistema localizzati (chiave composta lang + code).
-- Popolata dalla migration V2__dati_messaggi.sql.
CREATE TABLE messaggi_sistema (
    lang      VARCHAR(4)   NOT NULL,
    code      VARCHAR(50)  NOT NULL,
    messaggio VARCHAR(500) NOT NULL,

    CONSTRAINT pk_messaggi_sistema PRIMARY KEY (lang, code)
);

-- ============================================================================
-- AREA UTENTI
-- ============================================================================

CREATE TABLE utente (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email                    VARCHAR(320)  NOT NULL,      -- salvata sempre lowercase
    password_hash            VARCHAR(100)  NOT NULL,      -- BCrypt
    ruolo                    VARCHAR(10)   NOT NULL DEFAULT 'CLIENTE',
    nome                     VARCHAR(100),
    cognome                  VARCHAR(100),
    telefono                 VARCHAR(30),
    data_nascita             DATE,
    codice_fiscale           VARCHAR(16),
    indirizzo_predefinito_id BIGINT,                      -- FK aggiunta a fine file
    data_registrazione       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attivo                   BOOLEAN       NOT NULL DEFAULT TRUE,
    update_date            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_utente_email UNIQUE (email),
    CONSTRAINT uq_utente_cf    UNIQUE (codice_fiscale),
    CONSTRAINT chk_ruolo       CHECK (ruolo IN ('ADMIN', 'CLIENTE')),
    CONSTRAINT chk_dati_cliente CHECK (
        ruolo = 'ADMIN' OR (nome IS NOT NULL AND cognome IS NOT NULL)
    )
);

CREATE TABLE indirizzo (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    utente_id     BIGINT       NOT NULL,
    etichetta     VARCHAR(50),                 -- "Casa", "Ufficio"...
    destinatario  VARCHAR(200) NOT NULL,       -- nome che compare sul pacco
    via           VARCHAR(200) NOT NULL,
    civico        VARCHAR(20)  NOT NULL,
    cap           VARCHAR(10)  NOT NULL,
    citta         VARCHAR(100) NOT NULL,
    provincia     VARCHAR(50),
    nazione       VARCHAR(2)     NOT NULL DEFAULT 'IT',
    attivo        BOOLEAN      NOT NULL DEFAULT TRUE,   -- soft delete
    creation_date     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_indirizzo_utente FOREIGN KEY (utente_id) REFERENCES utente(id)
);

CREATE INDEX idx_indirizzo_utente ON indirizzo(utente_id);

-- ============================================================================
-- AREA PORTAFOGLIO
-- ============================================================================

CREATE TABLE portafoglio (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    utente_id     BIGINT        NOT NULL,
    saldo         NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    valuta        VARCHAR(3)      NOT NULL DEFAULT 'EUR',
    update_date TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_portafoglio_utente UNIQUE (utente_id),
    CONSTRAINT fk_portafoglio_utente FOREIGN KEY (utente_id) REFERENCES utente(id),
    CONSTRAINT chk_saldo CHECK (saldo >= 0)
);

CREATE TABLE conto_bancario (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    utente_id    BIGINT       NOT NULL,
    intestatario VARCHAR(200) NOT NULL,
    iban         VARCHAR(34)  NOT NULL,
    bic          VARCHAR(11),
    attivo       BOOLEAN      NOT NULL DEFAULT TRUE,   -- soft delete
    creation_date    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_conto_utente FOREIGN KEY (utente_id) REFERENCES utente(id)
);

CREATE INDEX idx_conto_utente ON conto_bancario(utente_id);

-- Ledger APPEND-ONLY: mai UPDATE su importo/commissione, solo transizioni di stato.
-- Lo storico transazioni dell'utente è una SELECT su questa tabella.
CREATE TABLE movimento_portafoglio (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    portafoglio_id      BIGINT        NOT NULL,
    tipo                VARCHAR(20)   NOT NULL,
    metodo              VARCHAR(10)   NOT NULL DEFAULT 'INTERNO',
    stato               VARCHAR(12)   NOT NULL DEFAULT 'IN_ATTESA',
    importo             NUMERIC(12,2) NOT NULL,   -- sempre positivo; il segno lo dà il tipo
    commissione         NUMERIC(12,2) NOT NULL DEFAULT 0.00,  -- PayPal: 5% + 0,35
    conto_bancario_id   BIGINT,                   -- obbligatorio per i prelievi
    ordine_id           BIGINT,                   -- FK aggiunta a fine file
    riferimento_esterno VARCHAR(100),             -- transaction id PayPal / CRO bonifico
    descrizione         VARCHAR(300),
    creation_date           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completion_date       TIMESTAMP,

    CONSTRAINT fk_movimento_portafoglio FOREIGN KEY (portafoglio_id) REFERENCES portafoglio(id),
    CONSTRAINT fk_movimento_conto       FOREIGN KEY (conto_bancario_id) REFERENCES conto_bancario(id),
    CONSTRAINT chk_mov_tipo   CHECK (tipo IN ('RICARICA','PRELIEVO','PAGAMENTO_ORDINE','RIMBORSO','RETTIFICA')),
    CONSTRAINT chk_mov_metodo CHECK (metodo IN ('PAYPAL','BONIFICO','INTERNO')),
    CONSTRAINT chk_mov_stato  CHECK (stato IN ('IN_ATTESA','COMPLETATO','RIFIUTATO','ANNULLATO')),
    CONSTRAINT chk_mov_importo     CHECK (importo > 0),
    CONSTRAINT chk_mov_commissione CHECK (commissione >= 0),
    CONSTRAINT chk_prelievo_conto  CHECK (tipo <> 'PRELIEVO' OR conto_bancario_id IS NOT NULL)
);

CREATE INDEX idx_movimento_portafoglio ON movimento_portafoglio(portafoglio_id, creation_date);
CREATE INDEX idx_movimento_stato       ON movimento_portafoglio(stato);

-- ============================================================================
-- AREA CATALOGO CARTE (modello Scryfall)
-- ============================================================================

-- tipo_set usa i valori testuali di Scryfall (expansion, core, masters,
-- commander, box, draft_innovation, promo, token...).
-- N.B. Secret Lair (SLD) ha set_type = 'box'.
CREATE TABLE espansione (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codice           VARCHAR(10)  NOT NULL,
    nome             VARCHAR(200) NOT NULL,
    tipo_set         VARCHAR(30)  NOT NULL,
    codice_set_padre VARCHAR(10),
    data_uscita      DATE,
    scryfall_id      UUID,
    icon_url        VARCHAR(500),
    numero_carte     INTEGER,

    CONSTRAINT uq_espansione_codice   UNIQUE (codice),
    CONSTRAINT uq_espansione_scryfall UNIQUE (scryfall_id)
);

CREATE INDEX idx_espansione_tipo ON espansione(tipo_set);

-- Livello ORACLE: la carta come concetto di gioco, unica per oracle_id.
-- colori / identita_colore: sottoinsieme ordinato di 'WUBRG' (es. 'WU', '').
-- legal / card_faces: JSON serializzato in TEXT, gestito con Jackson nel service.
CREATE TABLE carta (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    oracle_id       UUID          NOT NULL,
    nome            VARCHAR(300)  NOT NULL,
    costo_mana      VARCHAR(80),              -- es. '{1}{U}{U}'
    valore_mana     NUMERIC(4,1),             -- mezzi punti per gli Un-set
    tipo_riga       VARCHAR(200),             -- 'Creature — Human Wizard'
    testo_oracle    TEXT,
    forza           VARCHAR(10),              -- VARCHAR: esistono '*', '1+*'
    costituzione    VARCHAR(10),
    colori          VARCHAR(5)    NOT NULL DEFAULT '',
    identita_colore VARCHAR(5)    NOT NULL DEFAULT '',
    parole_chiave   VARCHAR(500),             -- CSV: 'Flying,Trample'
    legal           TEXT,                     -- JSON {"standard":"legal",...}
    card_faces      TEXT,                     -- JSON card_faces (bifronte/MDFC)

    CONSTRAINT uq_carta_oracle UNIQUE (oracle_id)
);

CREATE INDEX idx_carta_nome ON carta(nome);

-- Livello STAMPA: una pubblicazione della carta in un set.
-- Qui vivono tutti gli ID di integrazione esterna.
CREATE TABLE stampa (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    carta_id                BIGINT       NOT NULL,
    espansione_id           BIGINT       NOT NULL,
    numero_collezione       VARCHAR(20)  NOT NULL,
    rarita                  VARCHAR(10)  NOT NULL,
    artista                 VARCHAR(200),
    promo                   BOOLEAN      NOT NULL DEFAULT FALSE,
    has_non_foil              BOOLEAN      NOT NULL DEFAULT TRUE,
    has_foil                 BOOLEAN      NOT NULL DEFAULT FALSE,
    has_etched_foil               BOOLEAN      NOT NULL DEFAULT FALSE,
    effetti_cornice         VARCHAR(300),             -- CSV Scryfall frame_effects: 'showcase,extendedart'
    tipi_promo              VARCHAR(300),             -- CSV Scryfall promo_types: 'surgefoil,galaxyfoil'
    scryfall_id             UUID,
    multiverse_id           INTEGER,                  -- Gatherer
    cardmarket_id           INTEGER,                  -- Cardmarket idProduct
    cardtrader_blueprint_id INTEGER,                  -- Cardtrader blueprint
    image_url            VARCHAR(500),

    CONSTRAINT fk_stampa_carta      FOREIGN KEY (carta_id) REFERENCES carta(id),
    CONSTRAINT fk_stampa_espansione FOREIGN KEY (espansione_id) REFERENCES espansione(id),
    CONSTRAINT uq_stampa_numero     UNIQUE (espansione_id, numero_collezione),
    CONSTRAINT uq_stampa_scryfall   UNIQUE (scryfall_id),
    CONSTRAINT chk_rarita CHECK (rarita IN ('COMMON','UNCOMMON','RARE','MYTHIC','SPECIAL','BONUS'))
);

CREATE INDEX idx_stampa_carta ON stampa(carta_id);

-- Prezzi guida dalle fonti esterne, storicizzati (append-only)
CREATE TABLE prezzo_riferimento (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    stampa_id    BIGINT        NOT NULL,
    fonte        VARCHAR(20)   NOT NULL,
    finitura     VARCHAR(10)   NOT NULL DEFAULT 'NONFOIL',
    prezzo_trend NUMERIC(10,2),
    prezzo_medio NUMERIC(10,2),
    prezzo_min   NUMERIC(10,2),
    valuta       VARCHAR(3)      NOT NULL DEFAULT 'EUR',
    detection_date  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_prezzo_stampa FOREIGN KEY (stampa_id) REFERENCES stampa(id),
    CONSTRAINT chk_fonte    CHECK (fonte IN ('SCRYFALL','CARDMARKET','CARDTRADER')),
    CONSTRAINT chk_pr_finitura CHECK (finitura IN ('NONFOIL','FOIL','ETCHED'))
);

CREATE INDEX idx_prezzo_rif ON prezzo_riferimento(stampa_id, fonte, finitura);

-- ============================================================================
-- AREA CATALOGO COMMERCIALE (prodotto + SKU)
-- ============================================================================

-- SINGLE     -> stampa_id obbligatorio
-- BOOSTER / BOOSTER_BOX / SET_LOTTO / SIGILLATO -> di norma espansione_id
-- ACCESSORIO -> nessun riferimento
CREATE TABLE prodotto (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo_prodotto VARCHAR(15)  NOT NULL,
    nome          VARCHAR(300) NOT NULL,
    slug          VARCHAR(300) NOT NULL,
    descrizione   TEXT,
    espansione_id BIGINT,
    stampa_id     BIGINT,
    image_url  VARCHAR(500),
    attivo        BOOLEAN      NOT NULL DEFAULT TRUE,
    creation_date     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_prodotto_slug   UNIQUE (slug),
    CONSTRAINT uq_prodotto_stampa UNIQUE (stampa_id),
    CONSTRAINT fk_prodotto_espansione FOREIGN KEY (espansione_id) REFERENCES espansione(id),
    CONSTRAINT fk_prodotto_stampa     FOREIGN KEY (stampa_id) REFERENCES stampa(id),
    CONSTRAINT chk_tipo_prodotto CHECK (
        tipo_prodotto IN ('SINGLE','BOOSTER','BOOSTER_BOX','SET_LOTTO','SIGILLATO','ACCESSORIO')
    ),
    -- Solo (e sempre) i SINGLE hanno una stampa associata
    CONSTRAINT chk_single_stampa CHECK (
        (tipo_prodotto = 'SINGLE' AND stampa_id IS NOT NULL)
        OR (tipo_prodotto <> 'SINGLE' AND stampa_id IS NULL)
    )
);

CREATE INDEX idx_prodotto_tipo ON prodotto(tipo_prodotto);

-- Lo SKU è ciò che si vende: variante + prezzo + giacenza.
-- Per i non-SINGLE, condizione = 'NA' (sentinella: mai NULL, così il
-- vincolo UNIQUE resta standard e portabile).
CREATE TABLE magazzino_sku (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    prodotto_id   BIGINT        NOT NULL,
    condizione    VARCHAR(2)    NOT NULL DEFAULT 'NA',
    lingua        VARCHAR(2)      NOT NULL DEFAULT 'en',
    finitura      VARCHAR(10)   NOT NULL DEFAULT 'NONFOIL',
    prezzo        NUMERIC(10,2) NOT NULL,
    quantita      INTEGER       NOT NULL DEFAULT 0,
    attivo        BOOLEAN       NOT NULL DEFAULT TRUE,
    update_date TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_sku_prodotto FOREIGN KEY (prodotto_id) REFERENCES prodotto(id),
    CONSTRAINT uq_magazzino_sku_var UNIQUE (prodotto_id, condizione, lingua, finitura),
    CONSTRAINT chk_condizione   CHECK (condizione IN ('MT','NM','EX','GD','LP','PL','PO','NA')),
    CONSTRAINT chk_sku_finitura CHECK (finitura IN ('NONFOIL','FOIL','ETCHED')),
    CONSTRAINT chk_sku_prezzo   CHECK (prezzo >= 0),
    CONSTRAINT chk_sku_quantita CHECK (quantita >= 0)
);

CREATE INDEX idx_magazzino_sku_prodotto ON magazzino_sku(prodotto_id);

-- ============================================================================
-- AREA CARRELLO E ORDINI
-- ============================================================================

CREATE TABLE carrello (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    utente_id     BIGINT    NOT NULL,
    update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_carrello_utente UNIQUE (utente_id),
    CONSTRAINT fk_carrello_utente FOREIGN KEY (utente_id) REFERENCES utente(id)
);

CREATE TABLE voce_carrello (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    carrello_id BIGINT  NOT NULL,
    sku_id      BIGINT  NOT NULL,
    quantita    INTEGER NOT NULL,

    CONSTRAINT fk_vc_carrello FOREIGN KEY (carrello_id) REFERENCES carrello(id) ON DELETE CASCADE,
    CONSTRAINT fk_vc_sku      FOREIGN KEY (sku_id) REFERENCES magazzino_sku(id),
    CONSTRAINT uq_voce_carrello UNIQUE (carrello_id, sku_id),
    CONSTRAINT chk_vc_quantita  CHECK (quantita > 0)
);

-- L'ordine SNAPSHOTTA tutto: indirizzo e prezzi non dipendono più dalle
-- entità vive, così modifiche future non alterano lo storico.
CREATE TABLE ordine (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    utente_id         BIGINT        NOT NULL,
    stato             VARCHAR(20)   NOT NULL DEFAULT 'CREATO',
    totale            NUMERIC(12,2) NOT NULL,
    spese_spedizione  NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    -- Snapshot indirizzo di consegna
    sped_destinatario VARCHAR(200)  NOT NULL,
    sped_via          VARCHAR(200)  NOT NULL,
    sped_civico       VARCHAR(20)   NOT NULL,
    sped_cap          VARCHAR(10)   NOT NULL,
    sped_citta        VARCHAR(100)  NOT NULL,
    sped_provincia    VARCHAR(50),
    sped_nazione      VARCHAR(2)      NOT NULL DEFAULT 'IT',
    creation_date         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ordine_utente FOREIGN KEY (utente_id) REFERENCES utente(id),
    CONSTRAINT chk_stato_ordine CHECK (stato IN (
        'CREATO','SPEDITO','CONSEGNATO','NON_CONSEGNATO',
        'ANNULLATO','CANCELLATO','RESO_RICHIESTO','RIMBORSATO'
    )),
    CONSTRAINT chk_ordine_totale CHECK (totale >= 0)
);

CREATE INDEX idx_ordine_utente ON ordine(utente_id);
CREATE INDEX idx_ordine_stato  ON ordine(stato);

CREATE TABLE voce_ordine (
    id              BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ordine_id       BIGINT        NOT NULL,
    sku_id          BIGINT        NOT NULL,
    descrizione     VARCHAR(400)  NOT NULL,   -- snapshot leggibile
    prezzo_unitario NUMERIC(10,2) NOT NULL,   -- snapshot prezzo
    quantita        INTEGER       NOT NULL,

    CONSTRAINT fk_vo_ordine FOREIGN KEY (ordine_id) REFERENCES ordine(id),
    CONSTRAINT fk_vo_sku    FOREIGN KEY (sku_id) REFERENCES magazzino_sku(id),
    CONSTRAINT chk_vo_quantita CHECK (quantita > 0),
    CONSTRAINT chk_vo_prezzo   CHECK (prezzo_unitario >= 0)
);

CREATE INDEX idx_voce_ordine ON voce_ordine(ordine_id);

-- Audit trail dei cambi di stato (chi, quando, da cosa a cosa)
CREATE TABLE storico_stato_ordine (
    id          BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ordine_id   BIGINT      NOT NULL,
    stato_da    VARCHAR(20),
    stato_a     VARCHAR(20) NOT NULL,
    eseguito_da BIGINT,
    nota        VARCHAR(300),
    creation_date   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_sso_ordine FOREIGN KEY (ordine_id) REFERENCES ordine(id),
    CONSTRAINT fk_sso_utente FOREIGN KEY (eseguito_da) REFERENCES utente(id)
);

CREATE INDEX idx_storico_ordine ON storico_stato_ordine(ordine_id);

-- Recensione di un prodotto, consentita solo dopo la consegna di un
-- ordine che lo contiene (verifica nel service: ordine dell'utente,
-- stato CONSEGNATO, prodotto presente tra le voci).
-- Una sola recensione per (utente, prodotto); modifiche tracciate
-- da update_date. Moderazione tramite stato.
CREATE TABLE recensione (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    utente_id     BIGINT       NOT NULL,
    prodotto_id   BIGINT       NOT NULL,
    ordine_id     BIGINT       NOT NULL,     -- l'ordine consegnato che la giustifica
    voto          SMALLINT     NOT NULL,
    titolo        VARCHAR(150),
    testo         TEXT,
    stato         VARCHAR(12)  NOT NULL DEFAULT 'APPROVATA',
    creation_date TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_recensione_utente   FOREIGN KEY (utente_id)   REFERENCES utente(id),
    CONSTRAINT fk_recensione_prodotto FOREIGN KEY (prodotto_id) REFERENCES prodotto(id),
    CONSTRAINT fk_recensione_ordine   FOREIGN KEY (ordine_id)   REFERENCES ordine(id),
    CONSTRAINT uq_recensione_utente_prodotto UNIQUE (utente_id, prodotto_id),
    CONSTRAINT chk_recensione_voto  CHECK (voto BETWEEN 1 AND 5),
    CONSTRAINT chk_recensione_stato CHECK (stato IN ('IN_ATTESA','APPROVATA','RIFIUTATA'))
);

CREATE INDEX idx_recensione_prodotto ON recensione(prodotto_id);

-- ============================================================================
-- FK "circolari" aggiunte in coda (le tabelle referenziate ora esistono)
-- ============================================================================

-- Massimo UN indirizzo predefinito per utente: garantito strutturalmente
ALTER TABLE utente
    ADD CONSTRAINT fk_utente_ind_predef
    FOREIGN KEY (indirizzo_predefinito_id) REFERENCES indirizzo(id);

ALTER TABLE movimento_portafoglio
    ADD CONSTRAINT fk_movimento_ordine
    FOREIGN KEY (ordine_id) REFERENCES ordine(id);

-- ============================================================================
-- NOTE OPERATIVE
-- ----------------------------------------------------------------------------
-- 1. update_date: gestito da Hibernate con @UpdateTimestamp (niente trigger).
--    data_registrazione / creation_date: @CreationTimestamp.
--
-- 2. Email: normalizzare (trim + lowercase) nel service PRIMA di ogni
--    salvataggio e di ogni ricerca. Unico punto di normalizzazione.
--
-- 3. Concorrenza denaro/magazzino: aggiornare saldo e quantita SEMPRE in
--    transazione con @Lock(PESSIMISTIC_WRITE), ordinando gli id prima
--    dell'acquisizione (stesso pattern Fase 2). I CHECK sono l'ultima difesa.
--
-- 4. Ricarica PayPal: stato COMPLETATO immediato, commissione = 5% + 0,35.
--    Bonifico: stato IN_ATTESA finché l'admin non conferma (fino a 5 gg),
--    commissione 0. Prelievo: richiede conto_bancario_id (vincolo CHECK).
--
-- 5. Eliminazione indirizzi/conti: SOLO soft delete (attivo = FALSE).
--    Se si disattiva l'indirizzo predefinito, azzerare prima
--    utente.indirizzo_predefinito_id nel service.
--
-- 6. H2 nei test: usare MODE=PostgreSQL nell'URL. Mantenere i prefissi di
--    codici univoci per classe di test (pattern MZT/CRT/ORT... di Fase 2).
--
-- 7. Upgrade futuri solo-Postgres (quando si abbandona H2): migration
--    additive per pg_trgm (ricerca fuzzy), CITEXT, indici parziali.
-- ============================================================================