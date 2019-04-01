import React from 'react';
import { Admin, Resource } from 'react-admin';
import { createMuiTheme } from '@material-ui/core/styles';
import englishMessages from 'ra-language-english';

import Login from './auth/Login';
import AppLayout from './AppLayout';
import { Dashboard } from './dashboard';
import { EissCubesIcon, EissCubesList, EissCubesShow, EissCubesEdit } from './cubes';
import { CommandIcon, CommandList, CommandShow, CommandCreate } from './commands';
import { PropertyIcon, PropertyList, PropertyEdit, PropertyCreate } from './properties';
import createRealtimeSaga from "./rest/createRealtimeSaga";

import HttpService from './rest/HttpService';
import RestAdapter from './rest/restAdapter';
import AuthProvider from './rest/AuthProvider';
import addUploadFeature from './rest/addUploadFeature';

const apiUrl = (process.env.NODE_ENV === 'development') ? 'http://localhost:5000' : '/cube';
const tokenUrl = (process.env.NODE_ENV === 'development') ? 'http://localhost:4000/auth/token' : '/auth/token';
const refreshUrl = (process.env.NODE_ENV === 'development') ? 'http://localhost:4000/auth/refresh' :'/auth/refresh';

const http = new HttpService({ tokenUrl, refreshUrl });
const authProvider = new AuthProvider(http).authenticate;

export const dataProvider = addUploadFeature(new RestAdapter(apiUrl, http));

export const AppDateFormat = { year: 'numeric', month: '2-digit', day: '2-digit' };
export const AppDateTimeFormat = { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false };
export const DateTimeFormat = 'MM/dd/YYYY, HH:mm:ss';
export const DateTimeMomentFormat = 'MM/DD/YYYY, HH:mm:ss';

const theme = createMuiTheme({
	palette: {
		primary: {
			main: '#448ab6'
		},
		secondary: {
			main: '#448ab6'
		}
    }
});

const messages = {
    en: {
        'ra.action.upload': 'Upload',
        'ra.action.send': 'Send',
        'ra.message.request_sent': 'Request sent',
        ...englishMessages
    }
}

const i18nProvider = locale => messages[locale];
const realTimeSaga = createRealtimeSaga(dataProvider);

const App = () => (
	<Admin
		theme={ theme }
        appLayout={ AppLayout }
        dashboard={ Dashboard }
        loginPage={ Login }
        authProvider={ authProvider }
        dataProvider={ dataProvider }
        customSagas={ [realTimeSaga] }
        locale="en" 
        i18nProvider={i18nProvider}
    >
		<Resource options={{ label: 'EISS™Cubes' }}
			name="cubes"
			icon={ EissCubesIcon }
            list={ EissCubesList }
            show={ EissCubesShow }
            edit={ EissCubesEdit }
		/>
		<Resource options={{ label: 'Commands' }}
			name="commands"
			icon={ CommandIcon }
            list={ CommandList }
            show={ CommandShow }
            create={ CommandCreate }
		/>
		<Resource options={{ label: 'Properties' }}
			name="properties"
			icon={ PropertyIcon }
            list={ PropertyList }
            edit={ PropertyEdit }
            create={ PropertyCreate }
		/>
	</Admin>
);

export default App;
