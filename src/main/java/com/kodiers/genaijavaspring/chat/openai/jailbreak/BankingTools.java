package com.kodiers.genaijavaspring.chat.openai.jailbreak;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class BankingTools {

    @Tool(name = "get-account-balance", description = "Retrieves the balance of a given bank account")
    public String getAccountBalance(@ToolParam(description = "The ID of the bank account to retrieve balance for") String accountId) {
        if ("12345".equals(accountId)) {
            return "$ 5000";
        }
        return "Account not found";
    }
}
