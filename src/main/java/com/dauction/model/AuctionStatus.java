package com.dauction.model;

public enum AuctionStatus {
    /** Toujours en vente, visible dans le browse. */
    ACTIVE,
    /** Vendue - l'argent a déjà été transféré, rien à réclamer. */
    SOLD,
    /** Annulée par le vendeur (ou un admin) - objet en attente de retrait via /ah reclaim. */
    CANCELLED,
    /** Personne ne l'a achetée avant la date d'expiration - objet en attente de retrait. */
    EXPIRED,
    /** Objet d'une annonce CANCELLED/EXPIRED récupéré par le vendeur. */
    COLLECTED
}
