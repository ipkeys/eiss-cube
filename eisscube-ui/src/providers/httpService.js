import axios from 'axios';
import TokenManager from './tokenManager';
import {setApplication} from '../globalExports';

let tokenManager = null;
let authUrl = "";

//-----------------------------------------------------------------------------
// constructor
//-----------------------------------------------------------------------------
export default function HttpService(opts) {
    authUrl = opts.authUrl;

    tokenManager = new TokenManager({
        http: axios,
        tokenUrl: opts.tokenUrl || authUrl + "/token",
        refreshUrl: opts.refreshUrl || authUrl + "/refresh",
        // first three options are required or an exception is thrown
        tokenStorageKey: opts.tokenStorageKey
    });

    this.login = ({username, password}) => {
        return tokenManager.login(username, password);
    };

    this.logout = () => {
        tokenManager.logout();
        setApplication();
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

    this.getMFAsecret = ({username}) => {
        const options = {
            headers: {'Content-Type': 'application/json'}
        };
        return axios.post(authUrl + "/mfasecret", {username}, options);
    }

    this.activateMFA = ({username, mfaSecret, auth1, auth2}) => {
        const options = {
            headers: {'Content-Type': 'application/json'}
        };
        return axios.post(authUrl + '/activatemfa', {username, mfaSecret, auth1, auth2}, options);
    }

    this.deactivateMFA = ({username}) => {
        const options = {
            headers: {'Content-Type': 'application/json'}
        };
        return axios.post(authUrl + '/deactivatemfa', {username}, options);
    }

    this.processError = (error, prefix = "eiss") => {
        if (error.response) {
            if (error.response.status === "404") {
                console.log(error.response.data);
                return Promise.reject(new Error(""))
            }
            else if (error.response.status === "500") {
                return Promise.reject(new Error(prefix + ".server_error"));
            } 
            else {
                // Server should return a translatable key for bad requests
                return Promise.reject(new Error(prefix + "." + error.response.data.toLowerCase()));
            }
        }
        // If no `response` property, Error message is assumed to be translatable
        else if (error.message) {
            return Promise.reject(new Error(prefix + '.' + error.message));
        }
        // If no response at all
        else {
            return Promise.reject(new Error(prefix + ".no_response"));
        }
    };

    //----------------------------------------------------------------------------
    // HTTP methods which insert Authorization header with JWT.
    // All errors are processed and checked for authorization issues.
    //----------------------------------------------------------------------------
    this.request = (type, url, cfg = {}) => {
        return tokenManager.getToken()
        .then(
            (token) => {
                if (!cfg.headers) {
                    cfg.headers = { Accept: 'application/json' };
                }

                if (token) {
                    if (cfg.headers) {
                        cfg.headers.Authorization = ('Bearer ' + token);
                    }
                    else {
                        cfg.headers = { Authorization: 'Bearer ' + token };
                    }
                }

                console.log(type, url ,cfg);
                if (type === "post" || type === "put") {
                    const {body, method, ...opts} = cfg;
                    return axios[type](url, body, opts);
                }

                return axios[type](url, cfg);
            }, 
            (err) => {
                return this.processError(err, "eiss.auth");
            }
        );
    }

    this.get = (url, opts) => this.request("get", url, opts);

    this.post = (url, opts) => this.request("post", url, opts);

    this.put = (url, opts) => this.request("put", url, opts);

    this.delete = (url, opts) => this.request("delete", url, opts);
}
