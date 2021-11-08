import { AuthProvider, HttpError } from 'react-admin';
import { redirectLogin } from '../App';
import { development } from '../global';
import TokenManager from './TokenManager';

// The amount of time between checks
const CHECK_DURATION = 15; // seconds

// Track the duration of no activity
let _idleCounter = 0;

document.addEventListener("click", () => {
    _idleCounter = 0;
});

document.addEventListener("mousemove", () => {
    _idleCounter = 0;
});

document.addEventListener("keypress", () => {
    _idleCounter = 0;
});

export default function LoginService(tokenManager: TokenManager): AuthProvider {

    let interval = CHECK_DURATION * 1000;

    const start = () => {
        if (!development) {
            interval = window.setInterval(checkIdleTime, CHECK_DURATION * 1000);
        }
    }

    const logout = () => {
        tokenManager.removeToken();
        clearInterval(interval);
    }

    const checkIdleTime = () => {
        _idleCounter++;
        const timeoutDuration = tokenManager.getTimeoutDuration();
        if (timeoutDuration === null || _idleCounter * CHECK_DURATION >= Number(timeoutDuration) * 60) {
            logout();
            redirectLogin();
        }
    }

    const getUser = () => {
        const token = tokenManager.readToken();
        
        if (token) {
            const { user_id, group_id, role } = token;
            if (user_id && group_id && role) {
                return { user_id, group_id, role };
            }
        } 

        return null;
    }

    // If user is already logged in on page (re)load
    if (getUser() !== null) {
        start();
    }

    return ({
        login: ({username, password}: { username: string; password: string }) => {
            return tokenManager.login(username, password)
            .then(() => {
                start();
            })
            .catch((error) => {
                throw (error);
            });
        },

        logout: () => {
            logout();
            // route to redirect to after logout, defaults to /login
            return Promise.resolve();
        },

        getIdentity: () => {
            const user = getUser();
            return user ? Promise.resolve({id: user.user_id}) : Promise.reject();
        },

        checkError: (error: HttpError) => {
            const status = error.status;
            if (status === 401 || status === 403) {
                return Promise.reject();
            }
            return Promise.resolve();
        },

        checkAuth: () => {
            return getUser() ? Promise.resolve() : Promise.reject();
        },

        getPermissions: () => {
            const user = getUser();
            return user ? Promise.resolve(user) : Promise.reject();
        }
    });

}
