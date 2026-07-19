package com.betacom.mtgbazar.be.services.implementations.products;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.betacom.mtgbazar.be.dto.products.ImmagineDTO;
import com.betacom.mtgbazar.be.exceptions.MtgException;
import com.betacom.mtgbazar.be.services.IMessaggioServices;
import com.betacom.mtgbazar.be.services.interfaces.products.IImmagineServices;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Salvataggio immagini su filesystem. Il database conserva solo i
 * percorsi relativi (image_url); i byte vivono nella cartella upload,
 * esterna al jar (vedi ImageConfig).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImmagineImpl implements IImmagineServices {

    /** Whitelist: il content-type decide l'estensione, mai il nome originale.
     *  Niente SVG: puo' contenere script (stored XSS servito dal nostro dominio). */
    private static final Map<String, String> TIPI_IMMAGINE = Map.of(
            "image/jpeg", "jpg",
            "image/png",  "png",
            "image/webp", "webp");

    private final IMessaggioServices msg;

    @Value("${app.upload.dir}")
    private String uploadDir;
    private static final String PREFISSO_URL = "/immagini/";

    @PostConstruct
    private void initCartellaUpload() {
        try {
            Files.createDirectories(Path.of(uploadDir));
        } catch (IOException e) {
            // fail-fast: meglio un boot fallito che un upload fallito in produzione
            throw new IllegalStateException(
                    "Impossibile creare la cartella upload: " + uploadDir, e);
        }
    }

    @Override
    public ImmagineDTO salvaImmagine(MultipartFile file, String sottocartella) {
        if (file == null || file.isEmpty())
            throw new MtgException(msg.get("immagine.vuota"));

        String ext = TIPI_IMMAGINE.get(file.getContentType());
        if (ext == null)
            throw new MtgException(msg.get("immagine.tipo.non.valido"));

        // Nome DECISO dal server: niente path traversal, niente collisioni
        String nomeFile = UUID.randomUUID() + "." + ext;
        Path cartella = Path.of(uploadDir, sottocartella);

        try {
            Files.createDirectories(cartella);
            file.transferTo(cartella.resolve(nomeFile).toAbsolutePath());
        } catch (IOException e) {
            log.error("salvataggio immagine fallito in {}", sottocartella, e);
            throw new MtgException(msg.get("immagine.errore.salvataggio"));
        }
        log.debug("immagine salvata: {}/{}", sottocartella, nomeFile);
        return new ImmagineDTO("/immagini/" + sottocartella + "/" + nomeFile);
    }
    
    @Override
    public void eliminaImmagine(String percorsoRelativo) {
        if (percorsoRelativo == null || !percorsoRelativo.startsWith(PREFISSO_URL))
            return;   // esterni (es. URL assoluti) o null: niente da fare

        Path base = Path.of(uploadDir).toAbsolutePath().normalize();
        Path file = base.resolve(percorsoRelativo.substring(PREFISSO_URL.length()))
                        .normalize();

        // Difesa in profondita': il percorso risolto DEVE stare nel recinto
        if (!file.startsWith(base)) {
            log.warn("eliminaImmagine: percorso fuori recinto ignorato: {}", percorsoRelativo);
            return;
        }
        try {
            Files.deleteIfExists(file);
            log.debug("immagine eliminata: {}", percorsoRelativo);
        } catch (IOException e) {
            // best effort: un orfano su disco e' meglio di una transazione fallita
            log.warn("eliminazione immagine fallita: {}", percorsoRelativo, e);
        }
    }
    
}