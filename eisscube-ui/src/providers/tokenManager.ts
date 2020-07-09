import axios from 'axios';

// from: https://stackoverflow.com/questions/105034/create-guid-uuid-in-javascript
function guid() {
    function s4() {
        return Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1);
    }
    return s4()+s4()+'-'+s4()+'-'+s4()+'-'+s4()+'-'+s4()+s4()+s4();
}

type TokenData = { 
    accessToken: string, 
    tokenExpires: Date, 
    refreshToken: string, 
    refreshExpires: Date, 
    user: string, 
    group: string, 
    role: string, 
    device: string, 
    timeoutDuration: string
 } | null;

let win: Window, storage: Storage;

//------------------------------------------------------------------------------
// TokenManager - Get, refresh, store and manage tokens.
//
// token management and processing functions
//------------------------------------------------------------------------------
class TokenManager {
    tokenUrl: string;
    refreshUrl: string;
    tokenStorageKey: string;
    tokenData: TokenData = null;

    /**
     * Options are for test purposes:
     * @param win - the window object for location and replace()
     * @param storage - simulate session storage
    */ 
    constructor(opts: any) {
        this.tokenUrl = opts.tokenUrl;
        this.refreshUrl = opts.refreshUrl;

        if (!(this.tokenUrl && this.refreshUrl)) {
            throw new Error('TokenManager missing required option(s)');    
        }

        this.tokenStorageKey = opts.tokenStorageKey || 'sessionjwt';

        storage = opts.storage;
        if (!storage) {
            if (typeof sessionStorage !== 'undefined') {
                storage = sessionStorage;
            }
        }

        win = opts.win;
        if (!win) {
            if (typeof window !== 'undefined') {
                win = window;
            }
        }
    }

    saveTokenData = (data: any) => {
        storage.setItem(this.tokenStorageKey, JSON.stringify(data));
    }

    removeToken = () => {
        if (typeof storage !== 'undefined') {
            storage.removeItem(this.tokenStorageKey);
        }
        this.tokenData = null;
    }

    readTokenData = () => {
        const data = storage.getItem(this.tokenStorageKey);
        if (!data) {
            return;
        }
    
        try {
            this.tokenData = JSON.parse(data);
            if (this.tokenData === null) return;
            if (typeof this.tokenData.tokenExpires === 'string') {
                this.tokenData.tokenExpires = new Date(this.tokenData!.tokenExpires);
            }
            if (typeof this.tokenData.refreshExpires === 'string') {
                this.tokenData.refreshExpires = new Date(this.tokenData.refreshExpires);
            }
            return;
        } catch (error) {
            this.tokenData = null;
        }
    }
        
    getUser = () => {
        if (!this.tokenData) {
            this.readTokenData();
        }
        return this.tokenData ? this.tokenData!.user : null;
    };

    getGroup = () => {
        if (!this.tokenData) {
            this.readTokenData();
        }
        return this.tokenData ? this.tokenData.group : null;
    };

    getRole = () => {
        if (!this.tokenData) {
            this.readTokenData();
        }
        return this.tokenData ? this.tokenData.role : null;
    };

    getTimeoutDuration = () => {
        if (!this.tokenData) {
            this.readTokenData();
        }
        return this.tokenData ? this.tokenData.timeoutDuration : null;
    }

    getTokenExpiration = () => {
        if (!this.tokenData) {
            this.readTokenData();
        }
        return this.tokenData ? this.tokenData.tokenExpires : null;
    };

    getRefreshExpiration = () => {
        if (!this.tokenData) {
            this.readTokenData();
        }
        return this.tokenData ? this.tokenData.refreshExpires : null;
    };

    // Parse and store the received token data:
    // tokenResponse = { access_token: at, refresh_token: rt, expires: milliseconds }
    getNewTokenExpiration = (atoken: string) => {
        try {
            const parts = atoken.split('.');
            const str = win.atob(parts[1]);
            const token = JSON.parse(str);
            const tokenExpires = new Date(token.exp * 1000);
            return tokenExpires;
        } catch (err) {
            return null;
        }
    }

    getToken = () => {
        return new Promise((resolve, reject) => {
            if (!this.tokenData) {
                // Try to read token data before rejecting token
                this.readTokenData();
    
                if (!this.tokenData) {
                    return reject(new Error('expired'));
                }
            }
    
            if (this.tokenData.tokenExpires.getTime() > new Date().getTime()) {
                return resolve(this.tokenData.accessToken);
            }
    
            if (this.tokenData.refreshExpires.getTime() < new Date().getTime()) {
                return reject(new Error('expired'));
            }
    
            axios.post(
                this.refreshUrl, 
                {refresh_token: this.tokenData.refreshToken, device: this.tokenData.device}, 
                {headers: {'Content-Type': 'application/json'}}
            )
            .then((response) => {
                if (response?.data?.access_token) {
                    this.tokenData!.accessToken = response.data.access_token;
                    const newExpiration = this.getNewTokenExpiration(this.tokenData!.accessToken);
                    if (newExpiration) {
                        this.tokenData!.tokenExpires = newExpiration;
                        this.saveTokenData(this.tokenData);
                    }
                    return resolve(this.tokenData!.accessToken);
                } 
                else {
                    return reject(new Error('expired'));
                }
            })
            .catch((error) => reject(error));
        });
    };

    processToken = (tokenResponse: any, device: string) => {
        if (!(tokenResponse?.access_token)) {
            return false;
        }

        const accessToken = tokenResponse.access_token;
        const refreshToken = tokenResponse.refresh_token || null; // ok if not present, set to null for JSON storage
        // note the expires value is already in milliseconds, unlike the exp value in the token
        const refreshExpires = tokenResponse.expires ? new Date(tokenResponse.expires) : new Date();
        const timeoutDuration = tokenResponse.inactivity_duration;
        try {
            let parts = accessToken.split('.');
            const str = win.atob(parts[1]);
            const token = JSON.parse(str);
            const tokenExpires = new Date(token.exp * 1000);
            let user = '';
            let group = '';
            parts = token.sub.split('/');
            if (parts.length === 2) { 
                group = parts[0]; 
                user = parts[1]; 
            }
            const role = token.scope || '';
    
            this.tokenData = { accessToken, tokenExpires, refreshToken, refreshExpires, user, group, role, device, timeoutDuration };
            this.saveTokenData(this.tokenData);
    
            return true;
        }  catch (err) {
            return false;
        }
    }

    login = (username: string, password: string) => {
        return new Promise((resolve, reject) => {
            let device = guid();
            axios.post(
                this.tokenUrl, 
                {username, password, device}, 
                {headers: {'Content-Type': 'application/json'}}
            )
            .then(
                (response) => {
                    if (response && response.data) {
                        if (response.data.mfa || response.data.type) {
                            return resolve({data: response.data, device});
                        }
                        else if (!this.processToken(response.data, device)) {
                            return reject(new Error("eiss.auth.process"));
                        }
                        return resolve({});
                    }
                    return reject(new Error("eiss.no_response"));
                },
                (error) => reject(error)
            )
        });
    };

}

export default TokenManager;
