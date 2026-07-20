-- ============================================================================
-- PlaneswalkerBazar — V11: refresh token persistiti + messaggi auth
--
-- Refresh OPACHI (non JWT): stringa random a 256 bit consegnata al client
-- in un cookie HttpOnly; nel DB SOLO il suo SHA-256, mai il token in chiaro
-- (stesso principio delle password: un dump del DB non consegna sessioni).
--
-- 'famiglia' = catena di rotazione. A ogni /refresh il token usato viene
-- revocato e ne nasce uno nuovo nella stessa famiglia. Se arriva un token
-- GIA' revocato della famiglia, qualcuno sta riusando un token consumato
-- (furto): si revoca l'intera famiglia e si forza un nuovo login.
--
-- Portabilita' PG/H2 come da principi V1: niente UUID nativo (VARCHAR 36),
-- TIMESTAMP semplice come nel resto dello schema.
-- ============================================================================

CREATE TABLE refresh_token (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    utente_id     BIGINT      NOT NULL,
    token_hash    VARCHAR(64) NOT NULL,     -- SHA-256 esadecimale
    famiglia      VARCHAR(36) NOT NULL,     -- UUID testuale della catena
    scadenza      TIMESTAMP   NOT NULL,
    revocato      BOOLEAN     NOT NULL DEFAULT FALSE,
    creation_date TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_agent    VARCHAR(255),

    CONSTRAINT fk_rt_utente FOREIGN KEY (utente_id)
        REFERENCES utente(id) ON DELETE CASCADE,
    CONSTRAINT uq_rt_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_rt_utente   ON refresh_token(utente_id);
CREATE INDEX idx_rt_famiglia ON refresh_token(famiglia);
CREATE INDEX idx_rt_scadenza ON refresh_token(scadenza);   -- job di pulizia futuro

-- ---------------------------------------------------------------------------
-- Messaggi auth: anche i rifiuti della security parlano messaggi_sistema.
-- 401/403 della filter chain nascono PRIMA dei controller: li scrivono
-- l'AuthenticationEntryPoint e l'AccessDeniedHandler (JwtConfiguration).
-- ---------------------------------------------------------------------------
INSERT INTO messaggi_sistema (lang, code, messaggio) VALUES
('IT', 'auth.non.autenticato',  'Accesso non autorizzato: effettua il login'),
('IT', 'auth.accesso.negato',   'Non hai i permessi per questa operazione'),
('IT', 'auth.refresh.invalido', 'Sessione scaduta o non valida: effettua di nuovo il login');