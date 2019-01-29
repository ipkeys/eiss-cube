import Promise from 'bluebird';

//------------------------------------------------------------------------------
// token management and processing functions
//------------------------------------------------------------------------------

// from: https://stackoverflow.com/questions/105034/create-guid-uuid-in-javascript
function guid() {
    function s4() {
        return Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1);
    }
    return s4()+s4()+'-'+s4()+'-'+s4()+'-'+s4()+'-'+s4()+s4()+s4();
}

let http, tokenUrl, refreshUrl; // required props
let tokenStorageKey; // optional prop

let tokenData = null;
let storage, win, doc; // set in constructor after checking opts

// exported for testing
export function saveTokenData(data) {
    storage.setItem(tokenStorageKey, JSON.stringify(data));
}

// exported for testing
export function readTokenData() {
    const data = storage.getItem(tokenStorageKey);
    if (!data) {
        return null;
    }

    try {
        tokenData = JSON.parse(data);
        if (typeof tokenData.tokenExpires === 'string') tokenData.tokenExpires = new Date(tokenData.tokenExpires);
        if (typeof tokenData.refreshExpires === 'string') tokenData.refreshExpires = new Date(tokenData.refreshExpires);
        return tokenData;
    } catch (error) {
        tokenData = null;
    }
}

function removeToken() {
    if (typeof storage !== 'undefined') {
        storage.removeItem(tokenStorageKey);
    }
    tokenData = null;
}

//------------------------------------------------------------------------------
// Parse and store the received token data:
// tokenResponse = { access_token: at, refresh_token: rt, expires: milliseconds }
//------------------------------------------------------------------------------
function getNewTokenExpiration(atoken) {
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

export function processToken(tokenResponse, device) {
    if (!(tokenResponse && tokenResponse.access_token)) {
        return null;
    }

    let accessToken = tokenResponse.access_token;
    let refreshToken = tokenResponse.refresh_token || null; // ok if not present, set to null for JSON storage
    // note the expires value is already in milliseconds, unlike the exp value in the token
    let refreshExpires = tokenResponse.expires ? new Date(tokenResponse.expires) : new Date();
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

        tokenData = { accessToken, tokenExpires, refreshToken, refreshExpires, user, group, role, device };
        saveTokenData(tokenData);

        return tokenData;
    }  catch (err) {
        return null;
    }
}

//------------------------------------------------------------------------------
// TokenManager - Get, refresh, store and manage tokens.
//
// Options are for test purposes:
// win - the window object for location and replace()
// http - simulate axios posts
// storage - simulate session storage
//------------------------------------------------------------------------------
export default function TokenManager(opts) {
    if (!opts) throw new Error('TokenManager missing required option');    
  
    // babel does not allow object destructuring on variables already declared
    http = opts.http;
    tokenUrl = opts.tokenUrl;
    refreshUrl = opts.refreshUrl;
    if (!(http && tokenUrl && refreshUrl)) {
        throw new Error('TokenManager missing required option(s)');    
    }
    tokenStorageKey = opts.tokenStorageKey || 'sessionjwt';

    tokenData = null;

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

    doc = opts.doc;
    if (!doc) {
        if (typeof document !== 'undefined') {
            doc = document;
        }
    }

    this.getUser = () => {
        if (!tokenData) {
            readTokenData();
        }
        return tokenData ? tokenData.user : null;
    };

    this.getGroup = () => {
        if (!tokenData) {
            readTokenData();
        }
        return tokenData ? tokenData.group : null;
    };

    this.getRole = () => {
        if (!tokenData) {
            readTokenData();
        }
        return tokenData ? tokenData.role : null;
    };

    this.getTokenExpiration = () => {
        if (!tokenData) {
            readTokenData();
        }
        return tokenData ? tokenData.tokenExpires : null;
    };

    this.getRefreshExpiration = () => {
        if (!tokenData) {
            readTokenData();
        }
        return tokenData ? tokenData.refreshExpires : null;
    };

    this.getToken = () => {
        if (!tokenData) {
            readTokenData();
        }

        if (!tokenData) {
            return Promise.resolve(null);
        }

        if (tokenData.tokenExpires.getTime() > new Date().getTime()) {
            return Promise.resolve(tokenData.accessToken);
        }

        if (tokenData.refreshExpires.getTime() < new Date().getTime()) {
            removeToken();
            return Promise.resolve(null);
        }

        return http.post(refreshUrl, {refresh_token: tokenData.refreshToken, device: tokenData.device})
        .then((response) => {
            if (response && response.data && response.data.access_token) {
                tokenData.accessToken = response.data.access_token;
                const newExpiration = getNewTokenExpiration(tokenData.accessToken);
                if (newExpiration) {
                    tokenData.tokenExpires = newExpiration;
                    saveTokenData(tokenData);
                }
                return tokenData.accessToken;
            } else {
                removeToken();
                return Promise.resolve(null);
            }
        })
        .catch((error) => {
            removeToken();
            return Promise.resolve(null);
        });
    };

    this.login = ({ username: user, password }) => {
        return new Promise(function (resolve, reject) {
            let device = guid();
            http.post(tokenUrl, {user, password, refresh: true, device})
            .then(function (resp) {
            if (resp.data) {
                if (!processToken(resp.data, device)) {
                    return reject("error processing token");
                }
                resolve(user);
            }
            })
            .catch(function (error) {
                if (error.response) {
                    const resp = error.response;
                    if (resp.status === 401) {
                        return reject(new Error('Invalid credentials'));
                    } else if (resp && resp.data) {
                        reject(new Error('Unable to login at this time'));
                    } else {
                        reject(new Error('Login failed'));
                    }
                } else {
                    reject(new Error('No response from server'));
                }
            });
        });
    };

    this.logout = () => {
        removeToken();    
    }

    //----------------------------------------------------------------------------
    // this is used for change password--just see if a valid token
    // is received but not using it.
    //----------------------------------------------------------------------------
    this.validatePassword = (pass) => {
        return new Promise(function (resolve, reject) {
            if (!tokenData) {
                return reject(new Error('Not logged in'));
            }
            
            http.post(tokenUrl, {user: tokenData.user, password: pass, refresh: false})
            .then(function () {
                resolve(tokenData.user);
            })
            .catch(function (error) {
                if (error && error.response) {
                    const resp = error.response;
                    if (resp.status === 401) {
                        reject(new Error('Invalid credentials'));
                    } else if (resp.data) {
                        reject(new Error(resp.data));
                    } else {
                        reject(new Error('Login failed'));
                    }
                } else {
                    reject(new Error('No response from server'));
                }
            });
        });
    };

}
