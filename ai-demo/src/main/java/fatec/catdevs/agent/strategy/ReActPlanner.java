package fatec.catdevs.agent.strategy;

import org.springframework.ai.chat.client.ChatClient;

public class ReActPlanner implements PlanningStrategy {

    private final ChatClient chatClient;

    public ReActPlanner(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String createPlan(String userMessage) {
        return chatClient.prompt()
                .system("""
                        Você é um agente de conhecimentos atue como um assistente de pesquisa (jurídico, médico, financeiro).
                        Analise a solicitação do usuário e crie
                        um plano simples para resolver o problema.
                        O plano deve conter etapas numeradas.
                        Não execute a tarefa ainda.
                        Apenas apresente o plano.
                        Divida perguntas complexas em partes.
                        O planejamento para as resposta devem envolver a decomposição de consultas e síntese de resultados.
                        Use só o contexto fornecido, nunca invente dados ou citações.
                        Para todas as solicações inclua as fontes e links das pesquisas.
                        """)
                .user(userMessage)
                .call()
                .content();
    }
}