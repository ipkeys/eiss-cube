import AuthProvider from './authProvider';
import DataProvider from './dataProvider';
import HttpService from './httpService';
import LoginService from './loginService';
import TokenManager from './tokenManager';
import addUploadFeature from './addUploadFeature';

export const development = (process.env.NODE_ENV === 'development');

export const authUrl = development ? 'http://localhost:' + process.env.REACT_APP_AUTH_PORT + '/auth' : '/auth';
export const apiUrl =  development ? 'http://localhost:' + process.env.REACT_APP_CUBE_PORT : '/cube';

const tokenManager = new TokenManager({
    // first two options are required or an exception is thrown
    tokenUrl: authUrl + "/token",
    refreshUrl: authUrl + "/refresh",
    tokenStorageKey: 'sessionjwt'
});

export const loginService = new LoginService(tokenManager);
export const authProvider = new AuthProvider(loginService);
export const httpService = new HttpService(loginService);
export const dataProvider = addUploadFeature(new DataProvider(httpService).query);
export { i18nProvider } from './i18nProvider';
