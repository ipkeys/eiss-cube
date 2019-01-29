import Promise from 'bluebird';
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

    // makes sense of errors
    const processError = function (err) {
        if (err.response) {
            if (err.response.data) {
                return Promise.reject(new Error(err.response.data));
            } else {
                return Promise.reject(new Error(err.response.statusText || 'Unknown error'));
            }
        } else {
            return Promise.reject(new Error('No response from server'));
        }
    };

    // this is used for change password--just see if a valid token
    // is received but not using it.
    this.validatePassword = function (pass) {
        return tokenManager.validatePassword(pass);
    };

    this.login = function (user, pass) {
        return tokenManager.login(user, pass);
    };

    this.logout = function () {
        tokenManager.logout();
    };

    this.getUser = function () {
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
    function getConfig(token, config) {
        let cfg = config || {};
        if (token) {
            cfg.headers = { Authorization: 'Bearer ' + token };
        }
        return cfg;
    }

    this.get = function (url, config) {
        return tokenManager
            .getToken()
            .then((token) => {
                return axios.get(url, getConfig(token, config));
            })
            .catch(function (err) {
                return processError(err);
            });
    };

    this.post = function (url, data, config) {
        return tokenManager
            .getToken()
            .then((token) => {
                const body = data.body;
                return axios.post(url, body, getConfig(token, config));
            })
            .then(response => {
                return response;
            })
            .catch(function (err) {
                return processError(err);
            });
    };

    this.put = function (url, data, config) {
        return tokenManager
            .getToken()
            .then((token) => {
                const body = data.body;
                return axios.put(url, body, getConfig(token, config));
            })
            .then(response => {
                return response;
            })
            .catch(function (err) {
                return processError(err);
            });
    };

    this.delete = function (url, config) {
        return tokenManager
            .getToken()
            .then((token) => {
                return axios.delete(url, getConfig(token, config));
            })
            .then(response => {
                return response;
            })
            .catch(function (err) {
                return processError(err);
            });
    };
}
