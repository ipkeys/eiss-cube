import englishMessages from 'ra-language-english';

const messages = {
    en: {
        'ra.action.upload': 'Upload',
        'ra.action.send': 'Send',
        'ra.message.request_sent': 'Request sent',
        ...englishMessages,
        eiss: {
            auth: {
                expired: "Your session is expired",
                invalid: "Invalid credentials",
                failed: "Login failed",
                unable: "Unable to login at this time",
                process: "Error processing token"
            },
            mfa: {
                deactivated: "Deactivated MFA",
                fail: {
                    deactivated: "Could no deactivate MFA",
                    wrong: 'Failed to activate MFA. Wrong Authentication codes.',
                    server: '"MFA auth server error'
                }
            },
            server_error: "Internal error",
            no_response: "No response from server"
        },
        axios: {
            error: "Network Error"
        }
    }
}

export const i18nProvider = locale => messages[locale];
