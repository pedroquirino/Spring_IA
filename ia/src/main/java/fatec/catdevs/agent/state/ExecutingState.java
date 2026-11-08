package fatec.catdevs.agent.state;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * ConcreteState: redige a resposta usando SOMENTE o que a base de conhecimento trouxe.
 */
@Component
public class ExecutingState implements AgentState {

    private final ChatClient chatClient;
    private final FinalState next;

    public ExecutingState(ChatClient.Builder builder, FinalState next) {
        this.chatClient = builder.build();
        this.next = next;
    }

    @Override
    public void handle(AgentContext context) {
        String resposta = chatClient.prompt()
                .system("""
        Você é um assistente de base de conhecimento.

        REGRAS:
        - Se os trechos fornecidos responderem à pergunta, use-os e cite
          o documento de origem entre colchetes.
        - Se os trechos NÃO responderem, comece a resposta com
          "Não encontrei isso na base de conhecimento." e depois responda
          com seu conhecimento geral, deixando claro que não vem da base.
        - Nunca cite documentos que não estejam nos trechos fornecidos.
        - Em temas médicos, jurídicos ou financeiros, recomende consultar
          um profissional.
        - Responda em português.
        """)
                .user("""
        Histórico recente da conversa:
        %s

        Pergunta atual:
        %s

        Plano:
        %s

        Trechos da base de conhecimento:
        %s
        """.formatted(
                        context.getHistory(),
                        context.getUserMessage(),
                        context.getPlan(),
                        context.getKnowledge()))
                .call()
                .content();

        context.setResponse(resposta);
        context.setState(next);
    }

    @Override
    public String name() {
        return "Respondendo";
    }
}
