import { HttpError } from 'ra-core';
import { AuthProvider } from 'react-admin';
import { redirectLogin } from '../App';
import { development, authUrl } from '../global';
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

type Extended = {
	checkMfa: (mfatoken?: string) => Promise<void>
}

export default function LoginService(tokenManager: TokenManager): AuthProvider & Extended {

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
			.catch(error => {
				throw (error);
			});
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

		logout: () => {
			logout();
			return Promise.resolve();
		},

		getIdentity: () => {
			const user = getUser();
			return user ? Promise.resolve({id: user.user_id}) : Promise.reject();
		},

		getPermissions: () => {
			const user = getUser();
			return user ? Promise.resolve(user) : Promise.reject();
		},

		checkMfa: (mfatoken?: string) => {
			return new Promise<void>((resolve, reject) => {
				const token = tokenManager.readToken();
				const user_id = token?.user_id;
				const device = token?.device;

				user_id && device &&
				fetch(`${authUrl}/checkmfa`, {
					method: 'post',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify({ user_id, mfatoken, device })
				})
				.then(response => response.json())
				.then(data => tokenManager.processToken(data, device))
				.then(() => resolve())
				.catch(error => reject(error));
			});
		}
	});

}
