package fr.nextbanking.audit.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * Catalogue fermé des codes d'audit (AR-40, FR-026-01 à FR-026-03) : tout autre code est rejeté.
 */
public enum ActionAudit {
    LOGIN,
    LOGIN_FAILED,
    LOGOUT,
    ADMIN_DEACTIVATE,
    ADMIN_ACTIVATE,
    ADMIN_LOAN_APPROVE,
    ADMIN_LOAN_REFUSE,
    ADMIN_TRANSFER_RELEASE,
    ADMIN_TRANSFER_REJECT,
    VIREMENT,
    TRANSFER_EXECUTED,
    TRANSFER_AUTO_REJECT,
    TRANSFER_REJECT_INSUFFICIENT_FUNDS;

    /** Code du catalogue portant exactement ce nom (casse comprise), s'il existe. */
    public static Optional<ActionAudit> depuisCode(String code) {
        return Arrays.stream(values()).filter(action -> action.name().equals(code)).findFirst();
    }
}
