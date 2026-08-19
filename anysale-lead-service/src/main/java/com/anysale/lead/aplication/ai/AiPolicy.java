package com.anysale.lead.aplication.ai;

public record AiPolicy(boolean providerAvailable, boolean enabled, String model, int maxOutputTokens, Integer monthlyRequestLimit,
                       Long monthlyTokenLimit, long requestsUsed, long tokensUsed) {
    public boolean withinBudget() {
        return (monthlyRequestLimit == null || requestsUsed < monthlyRequestLimit)
                && (monthlyTokenLimit == null || tokensUsed < monthlyTokenLimit);
    }
}
