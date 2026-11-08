package fatec.catdevs.agent.memory;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class ConversationMemory {

    private static final int MAX_TROCAS = 10;

    private final List<String> trocas = new ArrayList<>();

    public synchronized void registrar(String pergunta, String resposta) {
        trocas.add("Usuário: " + pergunta + "\nAgente: " + resposta);
        while (trocas.size() > MAX_TROCAS) {
            trocas.remove(0);
        }
    }

    public synchronized String comoTexto() {
        return trocas.isEmpty() ? "(sem histórico)" : String.join("\n\n", trocas);
    }

    public synchronized void limpar() {
        trocas.clear();
    }
}