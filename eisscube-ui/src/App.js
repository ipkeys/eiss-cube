import React, { Fragment }from 'react';
import { Admin, Resource } from 'react-admin';
import { Route } from 'react-router-dom';
import { createMuiTheme } from '@material-ui/core/styles';

import cubeLayout from './Layout';
import Login from './Layout/Login';
import { Dashboard } from './dashboard';
import { EissCubesIcon, EissCubesList, EissCubesShow, EissCubesEdit } from './cubes';
import { LoraCubesIcon, LoraCubesList, LoraCubesShow, LoraCubesEdit } from './lora';
import { CommandIcon, CommandList, CommandShow, CommandCreate } from './commands';
import { PropertyIcon, PropertyList, PropertyEdit, PropertyCreate } from './properties';
import { ReportIcon, ReportList, ReportShow } from './reports';

import { development, authProvider, dataProvider, i18nProvider } from './providers';

const theme = createMuiTheme({
    sidebar: {
        width: 150
    },
	palette: {
		primary: {
			main: '#448ab6'
		},
		secondary: {
			main: '#448ab6'
		}
    },
    typography: {
        title: {
            fontWeight: 400,
        },
    },
    overrides: {
        MuiFilledInput: {
            root: {
                backgroundColor: 'rgba(0, 0, 0, 0.02)',
                '&$disabled': {
                    backgroundColor: 'rgba(0, 0, 0, 0.02 )',
                },
            },
        },
    }
});

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

export const SUPER = "securityadmin";

export const isSuperAdmin = permissions => (
    permissions && permissions.role === SUPER
);

const customRoutes = [
    <Route path={"/profile"} component={profile} />
];
  
const App = () => (
	<Admin disableTelemetry
        theme={ theme }
        i18nProvider={ i18nProvider }
        authProvider={ authProvider }
        dataProvider={ dataProvider }
        customRoutes={ customRoutes }
        layout={ cubeLayout }
        dashboard={ Dashboard }
        /* Prevents infinite loop in development*/
        {...(development ? {loginPage: Login}: {loginPage: redirectLogin} )}
    >
        {permissions => [
        <Resource name="groups" />,
        <Resource name="meters" />,
		<Resource options={{ label: 'EISS™Cubes' }}
			name="cubes"
			icon={ EissCubesIcon }
            list={ EissCubesList }
            show={ EissCubesShow }
            edit={ EissCubesEdit }
		/>,
		<Resource options={{ label: 'LoRa™Cubes' }}
			name="lora"
			icon={ LoraCubesIcon }
            list={ LoraCubesList }
            show={ LoraCubesShow }
            edit={ LoraCubesEdit }
		/>,
		<Resource options={{ label: 'Commands' }}
			name="commands"
			icon={ CommandIcon }
            list={ CommandList }
            show={ CommandShow }
            create={ CommandCreate }
		/>,
        <Resource options={{ label: 'Reports' }}
            name="reports"
            icon={ ReportIcon }
            list={ ReportList }
            show={ ReportShow }
        />,
        isSuperAdmin(permissions) 
        ?   <Resource options={{ label: 'Properties' }}
                name="properties"
                icon={ PropertyIcon }
                list={ PropertyList }
                edit={ PropertyEdit }
                create={ PropertyCreate }
            />
        : 
            null
        ]}
	</Admin>
);

export default App;
