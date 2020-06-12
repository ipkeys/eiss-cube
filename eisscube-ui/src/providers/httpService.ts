import axios from 'axios';
import { cfgObj, method } from './definitions';
import LoginService from './loginService';

//----------------------------------------------------------------------------
// HTTP methods which insert Authorization header with JWT.
// All errors are processed and checked for authorization issues.
//----------------------------------------------------------------------------
export default class HttpService {
    loginService: LoginService;

    constructor(loginService: LoginService) {
        this.loginService = loginService;
    }

    private request = (type: method, url: string, cfg?: cfgObj) => {
        return this.loginService.getToken()
        .then(
            (token: any) => {
                if (cfg === undefined) {
                    cfg = {} as cfgObj;
                }
                
                if (!cfg.headers) {
                    cfg.headers = { Accept: 'application/json' };
                }

                if (token) {
                    cfg.headers.Authorization = ('Bearer ' + token);
                }

                if (type === "post" || type === "put") {
                    const {body, ...opts} = cfg;
                    return axios[type](url, body, opts);
                }

                return axios[type](url, cfg);
            }, 
            (err: any) => {
                if (!err) {
                    throw (new Error("eiss.no_response"));
                }
                else {
                    this.loginService.logout();
                }
            }
        );
    }

    public get = (url: string, opts?: cfgObj) => this.request("get", url, opts);

    public post = (url: string, opts: cfgObj) => this.request("post", url, opts);

    public put = (url: string, opts: cfgObj) => this.request("put", url, opts);

    public delete = (url: string, opts?: cfgObj) => this.request("delete", url, opts);
}

