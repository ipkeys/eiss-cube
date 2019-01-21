import React from 'react';
import { Admin, Resource } from 'react-admin';
import { createMuiTheme } from '@material-ui/core/styles';
import englishMessages from 'ra-language-english';

import createRealtimeSaga from "./rest/createRealtimeSaga";
import DataProvider from './rest/DataProvider';

import { EissCubesIcon, EissCubesList, EissCubesShow, EissCubesEdit, EissCubesCreate } from './cubes';
import { CommandIcon, CommandList, CommandShow, CommandCreate } from './commands';

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
const realTimeSaga = createRealtimeSaga(DataProvider);

const App = () => (
	<Admin
		theme={ theme }
		title="EISS™Cube"
        dataProvider={ DataProvider }
        customSagas={[realTimeSaga]}
        locale="en" 
        i18nProvider={i18nProvider}
    >
		<Resource options={{ label: 'EISS™Cubes' }}
			name="cubes"
			icon={ EissCubesIcon }
            list={ EissCubesList }
            show={ EissCubesShow }
            edit={ EissCubesEdit }
            create={ EissCubesCreate }
		/>
		<Resource options={{ label: 'Commands' }}
			name="commands"
			icon={ CommandIcon }
            list={ CommandList }
            show={ CommandShow }
            create={ CommandCreate }
		/>
	</Admin>
);

export default App;
