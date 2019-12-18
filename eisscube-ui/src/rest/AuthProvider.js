import { 
    AUTH_LOGIN,
    AUTH_LOGOUT,
    AUTH_ERROR,
    AUTH_CHECK
} from 'react-admin';

export default function AuthProvider(http) {

    this.authenticate = (type, params) => {

        if (type === AUTH_LOGIN) {
            return http.login(params);
        }
  
        if (type === AUTH_LOGOUT) {
            return http.logout();
        }
  
        if (type === AUTH_ERROR) {
            const { status } = params;
            if (status === 401 || status === 403) {
                return Promise.reject();
            } else {
                return Promise.resolve();
            }
        }
  
        if (type === AUTH_CHECK) {
            return http.getUser() ? Promise.resolve() : Promise.reject();
        }
  
        return Promise.resolve();
    }; 

};
  