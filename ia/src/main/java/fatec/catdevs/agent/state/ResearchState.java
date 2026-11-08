package fatec.catdevs.agent.state;

import java.util.Arrays;
import java.util.LinkedHashSet;
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
    private static final int MAX_TRECHOS = 15;

    private static final Set<String> IGNORAR = Set.of(
            "para", "como", "qual", "quais", "quem", "onde", "quando", "porque", "sobre",
            "isso", "esse", "essa", "este", "esta", "existe", "fala", "falar", "tenho",
            "pode", "posso", "preciso", "gostaria", "explique", "explica", "diga",
            "base", "conhecimento", "documento", "documentos", "voce", "mais", "muito",
            "serve", "servem", "fazer", "funciona", "funcionam", "significa");

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
        Set<String> trechos = new LinkedHashSet<>();

        for (String termo : extrairTermos(context.getUserMessage())) {
            CommandResult resultado = invoker.run(new SearchKnowledgeCommand(base, termo));
            if (resultado.success()) {
                trechos.addAll(Arrays.asList(resultado.output().split("\n")));
            }
        }

        if (trechos.isEmpty()) {
            CommandResult lista = invoker.run(new ListDocumentsCommand(base));
            String documentos = lista.output();
            context.setKnowledge("Nenhum trecho relevante foi encontrado. " + documentos);
        } else {
            context.setKnowledge(trechos.stream().limit(MAX_TRECHOS).collect(Collectors.joining("\n")));
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
