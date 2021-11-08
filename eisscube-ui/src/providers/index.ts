import DataProvider from './DataProvider';
import HttpService from './HttpService';
import LoginService from './LoginService';
import TokenManager from './TokenManager';
import { authUrl, apiUrl } from '../global';

const tokenManager = new TokenManager({
    // first two options are required or an exception is thrown
    tokenUrl: `${authUrl}/token`,
    refreshUrl: `${authUrl}/refresh`,
    tokenStorageKey: 'sessionjwt'
});

export const http = new HttpService(tokenManager);
export const authProvider = LoginService(tokenManager);
export const dataProvider = DataProvider(apiUrl, http);
export { i18nProvider } from './i18nProvider';
