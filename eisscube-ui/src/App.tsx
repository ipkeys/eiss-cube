import { useEffect } from 'react';
import { Admin, Resource, CustomRoutes, Authenticated, defaultTheme, useGetIdentity, useAuthenticated } from 'react-admin';
import { Route } from 'react-router-dom';
import { authProvider, dataProvider, i18nProvider } from './providers';
import { development } from './global';

import EissLayout from './layout/EissLayout';
import LoginForm from './layout/LoginForm';
import { Dashboard } from './resources/dashboard';
import { CubeIcon, CubeList, CubeShow, CubeEdit } from './resources/cubes';
import { CommandIcon, CommandList, CommandShow, CommandCreate } from './resources/commands';
import { ReportIcon, ReportList, ReportShow } from './resources/reports';
import { PropertyIcon, PropertyList, PropertyShow, PropertyEdit, PropertyCreate } from './resources/properties';

import { isSuper } from './resources/common/Roles';

export const theme = {
	...defaultTheme,
	palette: {
		...defaultTheme.palette,
		primary: {
			main: '#448ab6'
		},
		secondary: {
			main: '#448ab6'
		}
	}
};

export const redirectLogin = () => {
	sessionStorage.setItem("application", "/ui/cube/")
	window.location.href = development ? '#/login' : "/ui/login/";
}

const Profile = () => {
	useAuthenticated();
	const { identity } = useGetIdentity();

	useEffect(() => {
		if (identity && identity.id) {
			window.location.href =`/ui/users/#/users/${identity.id}/show`;
		}
	}, [identity]);

	return <></>;
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

const App = () => (
	<Admin disableTelemetry
		theme={theme}
		i18nProvider={i18nProvider}
		authProvider={authProvider}
		dataProvider={dataProvider}
		layout={EissLayout}
		dashboard={Dashboard}
		{...(development ? {loginPage: LoginForm} : {loginPage: Login} )}
	>
		{permissions => [
			<Resource options={{label: 'EISS™ Cubes'}}
				name='cubes'
				icon={CubeIcon}
				list={CubeList}
				show={CubeShow}
				edit={CubeEdit}
			/>,
			<Resource options={{label: 'Commands'}}
				name='commands'
				icon={CommandIcon}
				list={CommandList}
				show={CommandShow}
				create={CommandCreate}
			/>,
			<Resource options={{label: 'Reports'}}
				name='reports'
				icon={ReportIcon}
				list={ReportList}
				show={ReportShow}
			/>,
			<Resource options={{label: 'Properties'}}
				name='properties'
				icon={PropertyIcon}
				list={PropertyList}
				show={PropertyShow}
				edit={(isSuper(permissions)) && PropertyEdit}
				create={(isSuper(permissions)) && PropertyCreate}
			/>,
			<Resource name='grps' />,
			<Resource name='meters' />,
			<CustomRoutes>
				<Route path='/profile' element={<Authenticated><Profile /></Authenticated>} />
			</CustomRoutes>
		]}
	</Admin>
);

export default App;
