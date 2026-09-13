package fatec.catdevs.agent.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import fatec.catdevs.agent.state.AgentContext;
import fatec.catdevs.agent.state.AgentStateType;
import fatec.catdevs.agent.strategy.PlanningStrategy;

@Service
public class AgentService {

    private final ChatClient chatClient;
    private final PlanningStrategy planningStrategy;

    public AgentService(
            ChatClient.Builder builder,
            PlanningStrategy planningStrategy
    ) {
        this.chatClient = builder.build();
        this.planningStrategy = planningStrategy;
    }

    public String chat(String userMessage) {

        AgentContext context = new AgentContext(userMessage);

        while (context.getState() != AgentStateType.FINAL) {

            switch (context.getState()) {

                case PLANNING -> planning(context);

                case RESEARCH -> research(context);

                case EXECUTING -> executing(context);

                case FINAL -> {
                }
            }
        }

        return context.getResponse();
    }

    private void planning(AgentContext context) {

                    System.out.println(">>> ENTROU NO PLANNING");

    

        String plan = planningStrategy.createPlan(
                context.getUserMessage()
        );

        context.setPlan(plan);
        context.setState(AgentStateType.RESEARCH);
    }

    private void research(AgentContext context) {

            System.out.println(">>> ENTROU NO RESEARCH");

        String research = chatClient.prompt()
                .system("""
                        Você está no estado RESEARCH de um agente.

                    Sua função é fornecer informações que serão usadas
                    posteriormente pelo estado EXECUTING.

                    NÃO responda diretamente ao usuário.
                    NÃO faça perguntas.
                    NÃO dê sugestões sobre como melhorar a resposta.

                    Analise o plano e produza somente as informações
                    necessárias para executar cada etapa.
                        """)
                .user("""
                    Solicitação:
                    %s

                    Plano:
                    %s
                        """.formatted(
                        context.getUserMessage(),
                        context.getPlan()
                ))
                .call()
                .content();

        context.setResponse(research);
        context.setState(AgentStateType.EXECUTING);
    }

    private void executing(AgentContext context) {

                    System.out.println(">>> ENTROU NO EXECUTING");


        String response = chatClient.prompt()
                .system("""
                        Você está na etapa de execução de um agente.

                        Resolva a solicitação do usuário utilizando
                        o plano e as informações obtidas na pesquisa.
                        """)
                .user("""
                        Solicitação:
                        %s

                        Plano:
                        %s

                        Pesquisa:
                        %s
                        """.formatted(
                        context.getUserMessage(),
                        context.getPlan(),
                        context.getResponse()
                ))
                .call()
                .content();

        context.setResponse(response);
        context.setState(AgentStateType.FINAL);{
                        System.out.println(">>> ENTROU NO FINAL");

        }
    }
}