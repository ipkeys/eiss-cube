import LoginService from './loginService';

export default class AuthProvider {
    loginService: LoginService;

    constructor(loginService: LoginService) {
        this.loginService = loginService;
    };

    public login = (params: any) => {
        return this.loginService.login(params);
    };

    public logout = () => {
        this.loginService.logout();
        return Promise.resolve();
    };

    public checkError = (params: any) => {
        const { status } = params;
        if (status === 401 || status === 403) {
            return Promise.reject();
        } else {
            return Promise.resolve();
        }
    };

    public checkAuth = () => {
        return this.loginService.getUser() ? Promise.resolve() : Promise.reject();
    };

    public getPermissions = () => {
        const user = this.loginService.getUser();
        return user ? Promise.resolve(user) : Promise.reject();
    };

};

