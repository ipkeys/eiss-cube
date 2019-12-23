import { 
    AUTH_LOGIN,
    AUTH_LOGOUT,
    AUTH_ERROR,
    AUTH_CHECK,
    AUTH_GET_PERMISSIONS
} from 'react-admin';

export default function AuthProvider(http) {

    this.authenticate = (type, params) => {

        switch (type) {
            case AUTH_LOGIN: {
                return http.login(params);
            }
            case AUTH_LOGOUT: {
                return http.logout();
            }
            case AUTH_ERROR: {
                const { status } = params;
                if (status === 401 || status === 403) {
                    return Promise.reject();
                } else {
                    return Promise.resolve();
                }
            }
            case AUTH_CHECK: {
                return http.getUser() ? Promise.resolve() : Promise.reject();
            }
            case AUTH_GET_PERMISSIONS: {
                const user = http.getUser();
                return (user) ? Promise.resolve(user) : Promise.reject();
            }
            default: {
                return Promise.reject();
            }
        }  

    }; 

};
  