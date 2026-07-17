package com.duapar.dauction.manager;

/**
 * Erreur "attendue" (mauvais usage, solde insuffisant, annonce introuvable...) destinée
 * à être affichée telle quelle à l'utilisateur par la commande qui l'a déclenchée.
 */
public class AuctionException extends RuntimeException {

    public AuctionException(String message) {
        super(message);
    }
}
