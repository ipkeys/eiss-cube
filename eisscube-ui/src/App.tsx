import { useEffect } from 'react';
import { defaultTheme, Admin, Resource } from 'react-admin';
import { Route } from 'react-router-dom';
import { createTheme, Theme } from '@material-ui/core/styles';
import { authProvider, dataProvider, i18nProvider } from './providers';
import { development } from './global';

import Layout from './layout';
import DevelopmentLogin from './layout/Login';
import Profile from './resources/Profile';
import { Dashboard } from './resources/dashboard';
import { CubeIcon, CubeList, CubeShow, CubeEdit } from './resources/cubes';
import { CommandIcon, CommandList, CommandShow, CommandCreate } from './resources/commands';
import { PropertyIcon, PropertyList, PropertyShow, PropertyEdit, PropertyCreate } from './resources/properties';
import { ReportIcon, ReportList, ReportShow } from './resources/reports';

import { isSuper } from './resources/common/Roles';

export const theme: Theme = createTheme({
	...defaultTheme,
	// @ts-ignore
	palette: {
		primary: {
			main: '#448ab6'
		},
		secondary: {
			main: '#448ab6'
		}
	},
	overrides: {
		MuiFilledInput: {
			root: {
				backgroundColor: 'rgba(0, 0, 0, 0.02)',
				'&$disabled': {
					backgroundColor: 'rgba(0, 0, 0, 0.02 )'
				},
			}
		}
	}
});

export const redirectLogin = () => {
	sessionStorage.setItem("application", "/ui/cube/")
	window.location.href = development ? '#/login' : "/ui/login/"; 
}

export const redirectHome = () => {
	sessionStorage.removeItem("application");
	window.location.href = development ? '#/' : "/ui/home/"; 
}

export const AppDateFormat = { year: 'numeric', month: '2-digit', day: '2-digit' };
export const AppDateTimeFormat = { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false };
export const DateTimeFormat = 'MM/dd/YYYY, HH:mm:ss';
export const DateTimeMomentFormat = 'MM/DD/YYYY, HH:mm:ss';

const Login = () => {
	useEffect(() => {
        redirectLogin();
    }, []);
    return <></>;
}

const customRoutes = [
	<Route path={"/profile"} component={Profile} />
];

const App = () => (
	<Admin disableTelemetry
		theme={ theme }
		i18nProvider={ i18nProvider }
		authProvider={ authProvider } 
		dataProvider={ dataProvider }
		customRoutes={ customRoutes }
		layout={ Layout }
		dashboard={ Dashboard }
		/* Prevents infinite loop in development*/
		{...(development ? {loginPage: DevelopmentLogin} : {loginPage: Login} )}
	>
		{permissions => [
		<Resource name="grps" />,
		<Resource name="meters" />,
		<Resource options={{ label: 'EISS™ Cubes' }}
			name="cubes"
			icon={ CubeIcon }
			list={ CubeList }
			show={ CubeShow }
			edit={ CubeEdit }
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
		<Resource 
			name="properties"
			options={{ label: 'Properties' }}
			icon={ PropertyIcon }
			list={ PropertyList } 
			show={ PropertyShow} 
			edit={ (isSuper(permissions)) && PropertyEdit } 
			create={ (isSuper(permissions)) && PropertyCreate } 
		/>
		]}
	</Admin>
);

export default App;
