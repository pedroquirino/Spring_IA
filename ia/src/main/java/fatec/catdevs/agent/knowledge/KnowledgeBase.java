package fatec.catdevs.agent.knowledge;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Base de conhecimento: uma pasta com arquivos .md/.txt.
 * É o "Receiver" do padrão Command: quem realmente faz o trabalho.
 */
@Component
public class KnowledgeBase {

    private static final int MAX_TRECHOS_POR_DOCUMENTO = 3;
    private static final int MAX_CARACTERES_POR_TRECHO = 300;

    private final Path pasta;

    public KnowledgeBase(@Value("${knowledge.path:./knowledge}") String caminho) {
        this.pasta = Path.of(caminho);
    }

    public List<String> listarDocumentos() throws IOException {
        if (!Files.isDirectory(pasta)) {
            return List.of();
        }
        try (Stream<Path> arquivos = Files.list(pasta)) {
            return arquivos
                    .filter(this::ehDocumento)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }

    public List<String> buscar(String termo) throws IOException {
        String alvo = normalizar(termo);
        List<String> trechos = new ArrayList<>();

        for (String documento : listarDocumentos()) {
            int encontrados = 0;
            for (String linha : lerLinhas(pasta.resolve(documento))) {
                if (normalizar(linha).contains(alvo)) {
                    trechos.add("[" + documento + "] " + cortar(linha.trim()));
                    if (++encontrados >= MAX_TRECHOS_POR_DOCUMENTO) {
                        break;
                    }
                }
            }
        }
        return trechos;
    }

    /** Remove acentos e põe em minúsculas, para "Padrão" casar com "padrao". */
    public static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private boolean ehDocumento(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase(Locale.ROOT);
        return Files.isRegularFile(arquivo) && (nome.endsWith(".md") || nome.endsWith(".txt"));
    }

    private List<String> lerLinhas(Path arquivo) throws IOException {
        try {
            return Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            return Files.readAllLines(arquivo, StandardCharsets.ISO_8859_1);
        }
    }

    private String cortar(String linha) {
        return linha.length() <= MAX_CARACTERES_POR_TRECHO
                ? linha
                : linha.substring(0, MAX_CARACTERES_POR_TRECHO) + "...";
    }
}
