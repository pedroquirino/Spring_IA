package fatec.catdevs.agent.command;

import java.io.IOException;
import java.util.List;

import fatec.catdevs.agent.knowledge.KnowledgeBase;

/** ConcreteCommand: busca um termo nos documentos da base de conhecimento. */
public class SearchKnowledgeCommand implements Command {

    private final KnowledgeBase base;
    private final String termo;

    public SearchKnowledgeCommand(KnowledgeBase base, String termo) {
        this.base = base;
        this.termo = termo;
    }

    @Override
    public String name() {
        return "Buscar \"" + termo + "\"";
    }

    @Override
    public CommandResult execute() {
        try {
            List<String> trechos = base.buscar(termo);
            if (trechos.isEmpty()) {
                return CommandResult.fail("Nenhum trecho encontrado para \"" + termo + "\".");
            }
            return CommandResult.ok(String.join("\n", trechos));
        } catch (IOException e) {
            return CommandResult.fail("Erro na busca: " + e.getMessage());
        }
    }
}
