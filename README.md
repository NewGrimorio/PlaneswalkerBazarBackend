# PlaneswalkerBazar — Backend

Backend REST di **PlaneswalkerBazar**, e-commerce per carte e prodotti sigillati di
*Magic: The Gathering*. Espone esclusivamente API JSON consumate dal frontend Angular
[`planeswalker-bazar-frontend`](../planeswalker-bazar-frontend).

Progetto realizzato nell'ambito di **Betacom Academy** — Fase 3.

---

## Stack

| Componente | Versione |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.0 (Spring 7.0.8) |
| Hibernate ORM | 7.4.1 |
| PostgreSQL | 18 (produzione) |
| H2 | 2.4 in `MODE=PostgreSQL` (test) |
| Flyway | migrazioni versionate V1 → V12 |
| Spring Security | JWT HS512 + refresh token opachi |
| SpringDoc | Swagger UI |

---

## Avvio rapido

### 1. Database

```sql
CREATE DATABASE db_jpa_bazar;
```

Flyway applica lo schema al primo avvio: non serve creare tabelle a mano.

### 2. Variabile d'ambiente (opzionale)

L'integrazione Cardtrader richiede un token personale, ottenibile dalle impostazioni
del proprio profilo su cardtrader.com:

```bash
export CARDTRADER_TOKEN=il-tuo-token
```

Senza token l'applicazione **parte comunque**: le funzioni Cardtrader rispondono
best-effort con un riepilogo a zero e un warning nei log.

### 3. Avvio

```bash
./mvnw spring-boot:run
```

Backend su **http://localhost:9090**, frontend atteso su **http://localhost:4200**.

- Swagger UI → `http://localhost:9090/swagger-ui.html`
- OpenAPI → `http://localhost:9090/v3/api-docs`

---

## Profili

| Profilo | Comportamento |
|---|---|
| `dev` | Identità dai token **attiva**, autorizzazione **disattivata** (`anyRequest().permitAll()`). Il frontend manda comunque il Bearer via interceptor. |
| `prod` | `STATELESS`, mappa permessi completa: `/api/public/**`, login/refresh/logout e Swagger aperti; `/api/admin/**` richiede `ROLE_ADMIN`; tutto il resto autenticato. |

I rifiuti passano da `AuthenticationEntryPoint` (401) e `AccessDeniedHandler` (403),
che rispondono JSON leggendo i testi da `messaggi_sistema`.

---

## Architettura

Ogni funzionalità attraversa gli stessi strati:

```
Entity (JPA)
  → Repository (Spring Data + named query JPQL)
    → Request DTO (validazione, gruppi Create/Update)
      → Service (@Transactional, regole di business)
        → Map (builder statico Entity → DTO)
          → Output DTO
            → Controller (REST)
```

### Modello dati: il catalogo a tre livelli

Ricalca il dominio Magic e il modello Scryfall:

- **`carta`** — la carta come *concetto di gioco*, unica per `oracle_id` (testo oracle, costo di mana, colori).
- **`stampa`** — una *pubblicazione* della carta in un set. Qui vivono tutti gli ID di
  integrazione esterna: `scryfall_id`, `multiverse_id`, `cardmarket_id`, `cardtrader_blueprint_id`.
- **`prodotto`** — l'entità commerciale (slug univoco, tipo, immagine).
- **`magazzino_sku`** — la *variante vendibile*: condizione × lingua × finitura, con prezzo e giacenza.

Vincoli chiave: `chk_single_stampa` (solo e sempre i `SINGLE` hanno una stampa);
`uq_magazzino_sku_var` sulla quaterna della variante, con sentinella `'NA'` al posto
di `NULL` per i non-SINGLE (in SQL `NULL` non è uguale a sé stesso).

### Principi trasversali

**Portabilità PostgreSQL ↔ H2.** Lo stesso file di migrazione gira su entrambi:
niente `ENUM` nativi (`VARCHAR` + `CHECK`), niente `CITEXT` (normalizzazione lowercase
nel service), niente `JSONB` (`TEXT` + Jackson), niente trigger (`@CreationTimestamp`).

**Storico immutabile.** L'ordine copia indirizzo e prezzi (`sped_*`, `prezzo_unitario`):
è un documento contabile, non una vista sui dati vivi.

**Append-only.** `movimento_portafoglio`, `prezzo_riferimento` e `storico_stato_ordine`
non vengono mai aggiornati: si aggiungono righe.

**Protezione a tre strati.** La UI previene → il service applica la regola parlando
da `messaggi_sistema` → i vincoli DB sono la rete di sicurezza finale.

**Concorrenza.** Ordine di lock fisso: prima `magazzino_sku` (con id ordinati per
prevenire deadlock), poi `portafoglio`. Dopo il `SELECT ... FOR UPDATE` serve
`em.refresh()` se le entità erano già nel persistence context.

---

## Integrazioni esterne

### Scryfall — catalogo e prezzi Cardmarket

`POST /api/admin/sync/{codiceSet}` importa un set: espansione → carte oracle → stampe
→ prodotti `SINGLE` (creati solo alla prima importazione) → rilevazione prezzi EUR.
Idempotente: rilanciarlo aggiorna, non duplica. Le stampe non più presenti su Scryfall
vengono marcate `orfana` per revisione admin.

### Cardtrader — blueprint

`POST /api/admin/sync/cardtrader` aggancia `cardtrader_blueprint_id` alle stampe
tramite lo `scryfall_id`, che entrambe le fonti conoscono. Scarica i blueprint di
**tutte** le espansioni Magic e costruisce un'unica mappa: così coprono anche le
varianti che Cardtrader tiene sotto espansioni-extra. Copertura ~99,5%.

### Tendenze prezzo

`GET /api/admin/magazzino/stampa/{id}/tendenze` restituisce, per ogni SKU della carta,
il prezzo corrente delle due fonti più la variazione rispetto allo snapshot precedente.

| Fonte | Granularità | Note |
|---|---|---|
| Cardtrader | **per-SKU** (condizione × lingua × finitura) | Marketplace filtrato server-side per foil/lingua, poi bucket per condizione |
| Cardmarket (via Scryfall) | **per-finitura** | Il per-condizione non è ottenibile: `prices.eur` non distingue la condizione |

Le due piattaforme usano scale di condizione diverse (7 gradi Cardmarket vs 5 Cardtrader):
la traduzione è isolata in `TraduttoreCondizioneCT`, con la corrispondenza dichiarata
come dato sull'enum `CondizioneCardtrader`.

---

## Migrazioni

| Versione | Contenuto |
|---|---|
| `V1` | Schema iniziale completo |
| `V2` | Messaggi di sistema |
| `V3` | Messaggi catalogo |
| `V4` | Ciclo di vita delle stampe (soft-delete, rilevamento orfani) |
| `V5` | Messaggi upload immagini |
| `V6` | Aggiunta mazzi |
| `V7` | Regole SKU per prodotti non-SINGLE |
| `V8` | Username |
| `V9` | Login a scelta (email o username) |
| `V10` | Immagine profilo utente |
| `V11` | Refresh token e messaggi auth |
| `V12` | Prezzi per-SKU Cardtrader/Cardmarket |

Le migrazioni applicate sono immutabili. Principio operativo: **parcheggiare** una
migrazione fuori dalla cartella finché non serve, invece di saltare numeri.

---

## Sicurezza

- **Access token**: JWT firmato HS512, vita 15 minuti. Validato dal Resource Server
  OAuth2 (`NimbusJwtDecoder`), non da codice custom. Il *subject* è l'**id utente**
  (immutabile), non lo username.
- **Refresh token**: stringa random opaca in cookie `HttpOnly`. Nel DB finisce solo
  il suo `SHA-256` — un dump del database non consegna sessioni attive.
- **Rilevamento furto**: la colonna `famiglia` traccia la catena di rotazione. Il riuso
  di un token già revocato brucia l'intera catena e forza un nuovo login.
- **Ownership check** nel service (`caricaProprio`: "l'ordine altrui non esiste"), che
  resta anche con la security attiva: difesa in profondità.

---

## Testing

```bash
./mvnw test
```

91 test, eseguibili anche in blocco dalla suite `PlaneswalkerBazarSuiteClass`.

Convenzioni: `@SpringBootTest @AutoConfigureMockMvc` su tutte le classi di integrazione
(condividono la cache del contesto ed evitano la doppia inizializzazione di H2);
fixture isolate per classe con codici univoci; verificate anche le transizioni
**illegali** della macchina a stati e i checkout concorrenti.

Una classe gira sotto `@ActiveProfiles("prod")` per esercitare la security che *impone*
(401/403/200). Le integrazioni esterne sono testate con `MockRestServiceServer`:
nessuna chiamata di rete reale nei test.

---

## Convenzioni

- Package plurali: `repositories.products`, `services.implementations.products`.
- Nomi di classe italiani per i service.
- Path REST in kebab-case: `conferma-consegna`, `set-predefinito`.
- `slug` (non `nomeUrl`), `BOOSTER_BOX` (non `BOX`).
- `TipoProdotto.SINGLE` creato **solo** dalla sync Scryfall, mai dal form admin.
- **Named query JPQL** in `jpa-named-queries.properties` (`NomeEntity.nomeMetodo=...`).
  Le derived query semplici restano firme nell'interfaccia: `@Query` non si usa.
- Lock e `@Modifying` come annotazioni sul metodo, mai nel file delle query.


# Comandi token

- openssl rand -base64 64 | tr -d '\r\n' > /tmp/jwt.txt
- wc -c /tmp/jwt.txt     # deve dire 88
- cat /tmp/jwt.txt | clip
- rm /tmp/jwt.txt                #Questo per generare il token jwt_secret

# Token Cardtrader API

- CARDTRADER_TOKEN=eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJjYXJkdHJhZGVyLXByb2R1Y3Rpb24iLCJzdWIiOiJhcHA6MjMxNDgiLCJhdWQiOiJhcHA6MjMxNDgiLCJleHAiOjQ5NDA0MDAzMDIsImp0aSI6Ijk0ODVhMGVjLTRhZWYtNDAwMS1hOTU2LTAyNDcxYWJhNjY5ZCIsImlhdCI6MTc4NDcyMzEwMiwibmFtZSI6Ik5lb2JsYWRlMTEgQXBwIDIwMjYwNzIyMTQyNTAyIn0.YMPMAdTIZpSzXjuYSr7AeFpbuIW9TLh9apAEbht6e-w-GDs4RIASqZcddSxSAsg1BWhWg9Yl2jBDC2Ot8SRUTxnQCkitY_TeidLJue12Fk67fGIIrd-dnrzOOWXU0OiesytsPXUFGN-OzITiQB04-d8s-vNbLvMorn7l2s5-AZ6I6Mta-2V_U5GfF6lUc5c6HX5eGOiXH2_yxn273kI46w0AAU5J-J1jwbJFfIoKoZJ6u_yomf8cvuWt1sB35x94guTA7eAG1BBvG5BwjH36QsaUaS8O4uHVMHuV0v8Hs4Tz3cDxVm7HXuKRGwHHWFkxKjE6c9kj86WnliXpZrt-0g
- Inserire il token di sopra in una variabile di ambiente chiamata CARDTRADER_TOKEN

---

## Lavori aperti

- Handler per `HandlerMethodValidationException` in `GlobalExceptionHandler`.
- `equals`/`hashCode` su `MessageID` (chiave composta) — warning Hibernate `HHH000038`.
- Job periodico MTGJSON per prezzi Cardmarket con storico a 90 giorni.
