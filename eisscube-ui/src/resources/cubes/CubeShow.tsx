import ChevronLeft from '@mui/icons-material/ChevronLeft';
import {
	Show,
	TopToolbar,
	ListButton,
	EditButton,
	useRecordContext
} from 'react-admin';
import {
	Avatar,
	CardHeader
} from '@mui/material';
import OnlineIcon from '@mui/icons-material/ThumbUp';
import OfflineIcon from '@mui/icons-material/ThumbDown';
import { red, green } from '@mui/material/colors';
import CubeMap from './CubeMap';
import RebootButton from './buttons/RebootButton';
import TestButton from './buttons/TestButton';
import SetupButton from './buttons/SetupButton';
import StatusField from './fields/StatusField';
import StartedAndLastPingField from './fields/StartedAndLastPingField';

const CubeShowToolbar = (props: any) => {
	const record = useRecordContext(props);

	let avatar;
	if (record && record.online) {
		avatar = <Avatar sx={{backgroundColor: green[500]}}><OnlineIcon /></Avatar>;
	} else {
		avatar = <Avatar sx={{backgroundColor: red[500]}}><OfflineIcon /></Avatar>;
	}

	return (
		<TopToolbar sx={{mt: 1}}>
			<CardHeader
				sx={{
					position: 'absolute',
					left: 0,
					padding: 0
				}}
				avatar={avatar}
				title={<StatusField {...props} />}
				subheader={<StartedAndLastPingField {...props}/>}
			/>
			<ListButton label='Back' icon={<ChevronLeft />} />
			<RebootButton />

			<TestButton />
			<SetupButton />
			<EditButton />
		</TopToolbar>
	);
};

const CubeShowTitle = () => {
	const record = useRecordContext();
	if (!record) return null;

	return <span>Manage EISS™Cube - {record.name}</span>;
};

const CubeShow = () => (
	<Show title={<CubeShowTitle />}
		actions={<CubeShowToolbar />}
	>
		<CubeMap />
	</Show>
);

export default CubeShow;
