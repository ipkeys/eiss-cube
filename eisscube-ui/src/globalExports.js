import React, {Fragment} from 'react';

import DataProvider from './providers/dataProvider';
import HttpService from './providers/httpService';
import AuthProvider from './providers/authProvider';
import addUploadFeature from './providers/addUploadFeature';

export const development = (process.env.NODE_ENV === 'development');

const authUrl = development ? 'http://localhost:' + process.env.REACT_APP_AUTH_PORT + '/auth' : '/auth';
const apiUrl =  development ? 'http://localhost:' + process.env.REACT_APP_CUBE_PORT : '/cube';

export const http = new HttpService({ authUrl });
export const authProvider = new AuthProvider(http).authenticate;
//export const dataProvider = DataProvider(apiUrl, http);
export const dataProvider = addUploadFeature(DataProvider(apiUrl, http));

export const setApplication = () => (
    sessionStorage.setItem("application", "/ui/cube/")
);

export const redirectLogin = () => {
    setApplication();
    window.location.href = "/ui/login/";
    return <Fragment />;
}

export const redirectHome = () => {
    sessionStorage.removeItem("application");
    window.location.href = "/ui/home/";
    return <Fragment />;
}

export const profile = () => {
    window.location.href = '/ui/users/#profile';
    return <Fragment />;
}

export const AppDateFormat = { year: 'numeric', month: '2-digit', day: '2-digit' };
export const AppDateTimeFormat = { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false };
export const DateTimeFormat = 'MM/dd/YYYY, HH:mm:ss';
export const DateTimeMomentFormat = 'MM/DD/YYYY, HH:mm:ss';

