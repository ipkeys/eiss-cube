import { fetchUtils } from 'react-admin';
import restAdapter from './restAdapter';
import addUploadFeature from './addUploadFeature';

export const API_URL = (process.env.NODE_ENV === 'development') ? 'http://localhost:10000' : '';
export const AUTH_TOKEN = 'Basic ' + localStorage.getItem('au');

const httpClient = (url, options = {}) => {
    options.user = {
        authenticated: true,
        token: AUTH_TOKEN
    }
    return fetchUtils.fetchJson(url, options);
}

const uploadCapableDataProvider = addUploadFeature(restAdapter(API_URL, httpClient));

export default (type, resource, params) =>
    new Promise(resolve =>
        setTimeout(() => resolve(uploadCapableDataProvider(type, resource, params)), 500)
    );
 