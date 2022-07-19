import DataProvider from './DataProvider';
import HttpService from './HttpService';
import LoginService from './LoginService';
import TokenManager from './TokenManager';
import { authUrl, apiUrl } from '../global';

const tokenManager = new TokenManager({
	tokenUrl: `${authUrl}/token`,
	refreshUrl: `${authUrl}/refresh`,
	tokenStorageKey: 'sessionjwt'
});

export const httpService = new HttpService(tokenManager);
export const authProvider = LoginService(tokenManager);
export const dataProvider = DataProvider(apiUrl, httpService);
export { i18nProvider } from './i18nProvider';
