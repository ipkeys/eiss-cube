import polyglotI18nProvider from 'ra-i18n-polyglot';
import englishMessages from 'ra-language-english';

const messages:any = {
    en: {
        ...englishMessages,
        'ra.action.upload': 'Upload',
        'ra.action.send': 'Send',
        'ra.message.request_sent': 'Request sent',
        eiss: {
            auth: {
                expired: 'Your session is expired',
                invalid: 'Invalid credentials',
                failed: 'Login failed',
                unable: 'Unable to login at this time',
                process: 'Error processing token'
            },
            mfa: {
                deactivated: 'Deactivated MFA',
                fail: {
                    deactivated: 'Could no deactivate MFA',
                    wrong: 'Failed to activate MFA. Wrong Authentication codes.',
                    server: 'MFA auth server error'
                }
            },
            file: {
                missing: "Missing file"
            },
            server_error: 'Internal error',
            no_response: 'No response from server'
        },
        axios: {
            error: 'Network Error'
        }
    }
}

export const i18nProvider = polyglotI18nProvider(locale => messages[locale], 'en');
