// from: https://stackoverflow.com/questions/105034/create-guid-uuid-in-javascript
function guid() {
	function s4() {
		return Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1);
	}
	return s4() + s4() + "-" + s4() + "-" + s4() + "-" + s4() + "-" + s4() + s4() + s4();
}

export type TokenData = {
	accessToken: string;
	tokenExpires: Date;
	refreshToken: string | null;
	refreshExpires: Date;
	user_id: string;
	group_id: string;
	role: string;
	device: string;
	timeoutDuration: string;
} | null;

type Opts = {
	win?: Window;
	storage?: Storage;
	tokenUrl: string;
	refreshUrl: string;
	tokenStorageKey?: string;
}

let win: Window, storage: Storage;

// ------------------------------------------------------------------------------
// TokenManager - Get, refresh, store and manage tokens.
// ------------------------------------------------------------------------------
class TokenManager {
	tokenUrl: string;
	refreshUrl: string;
	tokenStorageKey: string;
	tokenData: TokenData | null = null;

	/**
	 * Options are for test purposes:
	 * @param win - the window object for location and replace()
	 * @param storage - simulate session storage
	 */
	constructor(opts: Opts) {
		this.tokenUrl = opts.tokenUrl;
		this.refreshUrl = opts.refreshUrl;

		if (!(this.tokenUrl && this.refreshUrl)) {
			throw new Error("TokenManager missing required option(s)");
		}

		this.tokenStorageKey = opts.tokenStorageKey || "sessionjwt";

		if (!opts.storage) {
			if (typeof sessionStorage !== "undefined") {
				storage = sessionStorage;
			}
		} else {
			({ storage } = opts);
		}

		if (!opts.win) {
			if (typeof window !== "undefined") {
				win = window;
			}
		} else {
			({ win } = opts);
		}
	}

	saveToken = (data: TokenData) => {
		storage.setItem(this.tokenStorageKey, JSON.stringify(data));
		this.tokenData = data;
	};

	removeToken = () => {
		if (typeof storage !== "undefined") {
			storage.removeItem(this.tokenStorageKey);
		}
		this.tokenData = null;
	};

	readToken = () => {
		if (this.tokenData !== null) {
			return this.tokenData;
		}

		const data = storage.getItem(this.tokenStorageKey);
		if (!data) {
			return null;
		}

		try {
			this.tokenData = JSON.parse(data);
			if (this.tokenData === null) {
				return null;
			}
			if (typeof this.tokenData.tokenExpires === "string") {
				this.tokenData.tokenExpires = new Date(this.tokenData.tokenExpires);
			}
			if (typeof this.tokenData.refreshExpires === "string") {
				this.tokenData.refreshExpires = new Date(this.tokenData.refreshExpires);
			}
		} catch (error) {
			this.tokenData = null;
		}

		return this.tokenData;
	};

	getTimeoutDuration = () => {
		const tokenData = this.readToken();
		return tokenData ? tokenData.timeoutDuration : null;
	};

	getTokenExpiration = () => {
		const tokenData = this.readToken();
		return tokenData ? tokenData.tokenExpires : null;
	};

	getRefreshExpiration = () => {
		const tokenData = this.readToken();
		return tokenData ? tokenData.refreshExpires : null;
	};

	// Parse and store the received token data:
	// tokenResponse = { access_token: at, refresh_token: rt, expires: milliseconds }
	getNewTokenExpiration = (tokenData: TokenData) => {
		try {
			const parts = tokenData?.accessToken.split(".");
			if (parts === undefined) {
				return null;    
			}
			const str = win.atob(parts[1]);
			const token = JSON.parse(str);
			return new Date(token.exp * 1000);
		} catch (err) {
			return null;
		}
	};

	getToken = (): Promise<string> => {
		return new Promise((resolve, reject) => {
			const tokenData = this.readToken();

			if (tokenData === null) {
				return reject(new Error("expired"));
			}

			if (tokenData.tokenExpires.getTime() > new Date().getTime()) {
				return resolve(tokenData.accessToken);
			}

			if (tokenData.refreshExpires.getTime() < new Date().getTime()) {
				return reject(new Error("expired"));
			}

			// Update refresh token
			fetch(this.refreshUrl, {
				method: 'post',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ refresh_token: tokenData.refreshToken, device: tokenData.device }) 
			})
			.then(response => response.json())
			.then(data => {
				if (data.access_token) {
					tokenData.accessToken = data.access_token;
					const newExpiration = this.getNewTokenExpiration(tokenData);
					if (newExpiration !== null) {
						tokenData.tokenExpires = newExpiration;
						this.saveToken(tokenData);
					}
					return resolve(tokenData.accessToken);
				}
				return reject(new Error("expired"));
			})
			.catch(error => reject(error));
		});
	};

	processToken = (tokenResponse: any, device: string) => {
		if (!tokenResponse?.access_token) {
			return false;
		}

		const accessToken = tokenResponse.access_token;
		const refreshToken = tokenResponse.refresh_token || null; 
		// ok if not present, set to null for JSON storage
		// note the expires value is already in milliseconds, unlike the exp value in the token
		const refreshExpires = tokenResponse.expires ? new Date(tokenResponse.expires) : new Date();
		const timeoutDuration = tokenResponse.inactivity_duration;
		try {
			let parts = accessToken.split(".");
			const str = win.atob(parts[1]);
			const token = JSON.parse(str);
			const tokenExpires = new Date(token.exp * 1000);
			let user_id = "";
			let group_id = "";
			parts = token.sub.split("/");
			if (parts.length === 2) {
				group_id = parts[0];
				user_id = parts[1];
			}
			const role = token.scope || "";

			const tokenData = {
				accessToken,
				tokenExpires,
				refreshToken,
				refreshExpires,
				user_id,
				group_id,
				role,
				device,
				timeoutDuration,
			};
			this.saveToken(tokenData);

			return true;
		} catch (err) {
			return false;
		}
	};

	login = async (username: string, password: string): Promise<{ data: any; device: string } | void> => {
		return new Promise((resolve, reject) => {
			const device = guid();

			fetch(this.tokenUrl, {
				method: 'post',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ username, password, device }) 
			})
			.then(response => {
				if (response.ok){
					return response.json();
				} else {
					return {status: response.status, statusText: response.statusText};
				}
			})
			.then(data => {
				if (data.status === 401 && data.statusText === 'LOCKED') {
						return reject(data);
				}

				if (data.status === 401 && data.statusText === 'UNAUTHORIZED') {
					return reject(data);
				}

				if (data && this.processToken(data, device)) {
					if (data.mfa || (data.type && data.code)) {
						return reject(data);
					} else {
						return resolve();
					}
				} else {
					return reject(new Error("eiss.auth.process"));
				}
			})
			.catch(error => {
				reject(error);
			})
		});
	};
}

export default TokenManager;
