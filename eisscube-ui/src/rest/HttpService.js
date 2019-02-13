import axios from 'axios';
import TokenManager from './TokenManager';

let tokenManager = null;

//-----------------------------------------------------------------------------
// constructor
//-----------------------------------------------------------------------------
export default function HttpService(opts) {

    tokenManager = new TokenManager({
        http: axios,
        tokenUrl: opts.tokenUrl,
        refreshUrl: opts.refreshUrl,
        // first three options are required or an exception is thrown
        tokenStorageKey: opts.tokenStorageKey
    });

    this.login = ({username, password}) => {
        return tokenManager.login(username, password);
    };

    this.logout = () => {
        tokenManager.logout();
    };

    this.getUser = () => {
        const user = tokenManager.getUser();
        const group = tokenManager.getGroup();
        const role = tokenManager.getRole();
        if (user && group && role) {
            return {name: user, group, role};
        } else {
            return null;
        }
    };

    //----------------------------------------------------------------------------
    // All HTTP methods insert Authorization header with JWT.
    // All errors are processed and checked for authorization issues.
    //----------------------------------------------------------------------------
    const processError = (error) => {
        if (error.message) {
            return Promise.reject(error.message);
        }

        if (error.response) {
            if (error.response.data) {
                return Promise.reject(new Error(error.response.data));
            } else {
                return Promise.reject(new Error(error.response.statusText || 'Unknown error'));
            }
        } else {
            return Promise.reject(new Error('No response from server'));
        }
    };

    const getConfig = (token, options) => {
        let cfg = options || {};
        if (token) {
            cfg.headers = { Authorization: 'Bearer ' + token };
        }
        return cfg;
    };

    this.get = (url, options) => {
        return tokenManager.getToken()
        .then((token) => {
            return axios.get(url, getConfig(token, options));
        })
        .catch(function (err) {
            return processError(err);
        });
    };

    this.post = (url, options) => {
        return tokenManager.getToken()
        .then((token) => {
            const body = options.body;
            return axios.post(url, body, getConfig(token, options));
        })
        .then(response => {
            return response;
        })
        .catch(function (err) {
            return processError(err);
        });
    };

    this.put = (url, options) => {
        return tokenManager.getToken()
        .then((token) => {
            const body = options.body;
            return axios.put(url, body, getConfig(token, options));
        })
        .then(response => {
            return response;
        })
        .catch(function (err) {
            return processError(err);
        });
    };

    this.delete = (url, options) => {
        return tokenManager.getToken()
        .then((token) => {
            return axios.delete(url, getConfig(token, options));
        })
        .then(response => {
            return response;
        })
        .catch(function (err) {
            return processError(err);
        });
    };
}
