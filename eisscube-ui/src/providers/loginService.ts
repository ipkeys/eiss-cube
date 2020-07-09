import { setApplication } from '../App';
import ActivityTimeout from './timeoutService';
import TokenManager from './tokenManager';

export default class LoginService {
    tokenManager: TokenManager;
    activityTimeout: ActivityTimeout;

    constructor(tokenManager: TokenManager) {
        this.tokenManager = tokenManager;
        this.activityTimeout = new ActivityTimeout(this, tokenManager);
    }

    // This function is for development
    login = ({username, password}: any) => {
        this.activityTimeout.start();
        return this.tokenManager.login(username, password);
    };
    
    logout = () => {
        this.tokenManager.removeToken();
        this.activityTimeout.cancel();
        setApplication();
    };

    getUser = () => {
        const name = this.tokenManager.getUser();
        const group = this.tokenManager.getGroup();
        const role = this.tokenManager.getRole();
        if (name && group && role) {
            return {name, group, role};
        } else {
            return null;
        }
    };

    getToken = () => {
        return this.tokenManager.getToken();
    }

}

