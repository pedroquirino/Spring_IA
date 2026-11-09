package fatec.catdevs.agent.state;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import fatec.catdevs.agent.command.CommandInvoker;
import fatec.catdevs.agent.command.CommandResult;
import fatec.catdevs.agent.command.ListDocumentsCommand;
import fatec.catdevs.agent.command.SearchKnowledgeCommand;
import fatec.catdevs.agent.knowledge.KnowledgeBase;

/** ConcreteState: consulta a base de conhecimento executando Commands. */
@Component
public class ResearchState implements AgentState {

    private static final int MAX_TERMOS = 5;
    private static final int MAX_TRECHOS = 10;

    private static final Set<String> IGNORAR = Set.of(
            "para", "como", "qual", "quais", "quem", "onde", "quando", "porque", "sobre",
            "isso", "esse", "essa", "este", "esta", "existe", "fala", "falar", "tenho",
            "pode", "posso", "preciso", "gostaria", "explique", "explica", "diga",
            "base", "conhecimento", "documento", "documentos", "voce", "mais", "muito",
            "serve", "servem", "fazer", "funciona", "funcionam", "significa",
            "devo", "deve", "devem", "estou", "tempo", "procurar", "algum", "alguma",
            "quanto", "quanta", "quantos", "ajuda", "ajudar", "feita", "feito", "pela", "pelo",
            // NOVAS: palavras de perguntas sobre a própria base
            "seus", "suas", "liste", "listar", "lista", "conhecimentos", "assunto", "assuntos",
            "tema", "temas", "sabe", "saber", "conteudo", "arquivos", "possui", "disponivel",
            "disponiveis", "voces");

    private final CommandInvoker invoker;
    private final KnowledgeBase base;
    private final ExecutingState next;

    public ResearchState(CommandInvoker invoker, KnowledgeBase base, ExecutingState next) {
        this.invoker = invoker;
        this.base = base;
        this.next = next;
    }

    @Override
    public void handle(AgentContext context) {
        Set<String> termos = extrairTermos(context.getUserMessage());

        // NOVO: sem nenhum termo útil, a pergunta é sobre a própria base
        // ("qual a sua base de conhecimento?", "liste os seus conhecimentos").
        // Nesse caso entregamos o catálogo (arquivo + título) em vez de buscar.
        if (termos.isEmpty()) {
            try {
                String catalogo = base.catalogo();
                if (!catalogo.isBlank()) {
                    context.setKnowledgeFound(true);
                    context.setKnowledge("A pergunta é sobre a própria base. "
                            + "Estes são os documentos disponíveis (arquivo e assunto):\n" + catalogo);
                    context.setState(next);
                    return;
                }
            } catch (IOException e) {
                // se falhar, segue o fluxo normal abaixo
            }
        }

        // trecho -> quantos termos da pergunta ele contém (mais termos = mais relevante)
        Map<String, Integer> pontos = new LinkedHashMap<>();

        for (String termo : termos) {
            CommandResult resultado = invoker.run(new SearchKnowledgeCommand(base, termo));
            if (resultado.success()) {
                for (String trecho : Arrays.asList(resultado.output().split("\n"))) {
                    pontos.merge(trecho, 1, Integer::sum);
                }
            }
        }

        // ordena do mais relevante para o menos (sort é estável: empates mantêm a ordem de achado)
        java.util.List<String> trechos = pontos.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(MAX_TRECHOS)
                .collect(Collectors.toList());

        context.setKnowledgeFound(!trechos.isEmpty());

        if (trechos.isEmpty()) {
            CommandResult lista = invoker.run(new ListDocumentsCommand(base));
            String documentos = lista.output();
            context.setKnowledge("Nenhum trecho relevante foi encontrado. " + documentos);
        } else {
            context.setKnowledge(String.join("\n", trechos));
        }

        context.setState(next);
    }

    @Override
    public String name() {
        return "Pesquisando";
    }

    private Set<String> extrairTermos(String mensagem) {
        Set<String> termos = new LinkedHashSet<>();
        for (String palavra : KnowledgeBase.normalizar(mensagem).split("[^\\p{L}\\p{N}]+")) {
            if (palavra.length() >= 4 && !IGNORAR.contains(palavra)) {
                termos.add(palavra);
            }
            if (termos.size() == MAX_TERMOS) {
                break;
            }
        }
        return termos;
    }
}