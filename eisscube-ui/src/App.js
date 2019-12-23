import React from 'react';
import { Admin, Resource } from 'react-admin';
import { Route } from 'react-router-dom';
import { createMuiTheme } from '@material-ui/core/styles';
//import englishMessages from 'ra-language-english';

import Login from './auth/Login';
//import AppLayout from './AppLayout';
import { Dashboard } from './dashboard';
import { EissCubesIcon, EissCubesList, EissCubesShow, EissCubesEdit } from './cubes';
import { CommandIcon, CommandList, CommandShow, CommandCreate } from './commands';
import { PropertyIcon, PropertyList, PropertyEdit, PropertyCreate } from './properties';
import createRealtimeSaga from "./providers/createRealtimeSaga";

import {development, authProvider, dataProvider, profile, redirectLogin} from './globalExports';
import {i18nProvider} from './providers/i18nProvider';
import customLayout from './Layout';

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

const realTimeSaga = createRealtimeSaga(dataProvider);

const customRoutes = [
    <Route path={"/profile"} component={profile} />
];
  
const App = () => (
	<Admin
        theme={ theme }
        locale="en" i18nProvider={i18nProvider}
        authProvider={ authProvider }
        dataProvider={ dataProvider }
        customRoutes={customRoutes}
        customSagas={ [realTimeSaga] }
        appLayout={ customLayout }
        dashboard={ Dashboard }
        /* Prevents infinite loop in development*/
        {...(development ? {loginPage: Login}: {loginPage: redirectLogin} )}
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
		<Resource name="groups" />
	</Admin>
);

export default App;
