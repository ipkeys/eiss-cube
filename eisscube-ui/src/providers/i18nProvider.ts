import polyglotI18nProvider from 'ra-i18n-polyglot';
import { TranslationMessages } from 'react-admin';
import englishMessages from "ra-language-english";

export const prefix = 'eiss';

const customEnglishMessages: TranslationMessages = {
    ...englishMessages,
    [prefix]: {
        auth: {
            expired: "Your session is expired",
            invalid: "Invalid credentials",
            failed: "Login failed",
            unable: "Unable to login at this time",
            process: "Error processing token",
            unlocked: "Account unlocked",
            unable_unlock: "Unable to unlock at this time"
        },
        mfa: {
            activated: "Multi-Factor Authentication activated",
            deactivated: "Multi-Factor Authentication deactivated",
            fail: {
                deactivated: "Could no deactivate MFA",
                wrong: 'Failed to activate MFA. Wrong Authentication codes.',
                server: '"MFA auth server error'
            }
        },
        server_error: "Internal error",
        no_response: "No response from server"
    }
}

export const i18nProvider = polyglotI18nProvider(locale => customEnglishMessages, 'en');
