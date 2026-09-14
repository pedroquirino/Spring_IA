package com.example.aidemo.services;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.client.RestClient;

public class WebSearchTool {

            private final RestClient restClient;

            public WebSearchTool(RestClient.Builder builder){
            this.restClient = builder.baseUrl("https://api.tavily.com").build();
            }


            }




    

