import { useMediaQuery, Theme } from '@mui/material';
import {
	downloadCSV,
	List,
	Filter,
	Datagrid,
	TextField,
	SearchInput,
	ReferenceField,
	ReferenceInput,
	SelectInput,
	AutocompleteInput,
	ShowButton,
	DateField,
	SimpleList,
	useRefresh,
	usePermissions
} from 'react-admin';
import jsonExport from 'jsonexport/dist';
import moment from 'moment';
import { green, red } from '@mui/material/colors';
import { AppDateTimeFormat, DateTimeMomentFormat } from '../../App';
import { isSuper } from '../common/Roles';
import DeviceTypeField from './fields/DeviceTypeField';
import StatusField from './fields/StatusField';
import useRecursiveTimeout from '../../useRecursiveTimeout';

const exportCubeList = (data: any) => {
	const records = data.map((record: any) => ({
		deviceID: record.deviceID,
		name: record.name,
		online: record.online,
		lastPing: moment(record.lastPing).format(DateTimeMomentFormat),
		timeStarted: moment(record.timeStarted).format(DateTimeMomentFormat)
	}));

	jsonExport(records, {
		headers: ['deviceID', 'name', 'online', 'timeStarted', 'lastPing']
		}, (_err: any, csv: any) => {
			downloadCSV(csv, 'EISS™Cubes');
		}
	);
};

const CubeListFilter = (props: any) => {
	const { permissions } = usePermissions();

	return (
		<Filter {...props}>
			<SearchInput source='q' alwaysOn />
			<SelectInput source='online' label='Status' margin='dense' choices={[
				{ id: true, name: 'ONLINE' },
				{ id: false, name: 'OFFLINE' }
			]} />
			{isSuper(permissions) &&
				<ReferenceInput source="group_id" reference="grps" sort={{ field: 'displayName', order: 'ASC' }} allowEmpty >
					<AutocompleteInput optionText='displayName' />
				</ReferenceInput>
			}
		</Filter>
	);
};

const CubeList = (props: any) => {
	const isSmall = useMediaQuery<Theme>(theme => theme.breakpoints.down('sm'));
	const { permissions } = usePermissions();
	const { bulkActionButtons } = props;
	const refresh = useRefresh();

	useRecursiveTimeout(() => refresh, 10000);

	return (
		<List title='EISS™Cubes' {...props}
			filters={<CubeListFilter />}
			sort={{ field: 'name', order: 'ASC' }}
			perPage={10}
			exporter={exportCubeList}
			{...(isSuper(permissions) ? {bulkActionButtons } : {bulkActionButtons: false})}
		>
			{isSmall ? (
				<SimpleList
					linkType='show'
					primaryText={record => record.name}
					secondaryText={record => record.online === true ? <span style={{ color: green[500] }}>ONLINE</span> : <span style={{ color: red[500] }}>OFFLINE</span>}
				/>
			) : (
				<Datagrid
					sx={{
						'& .RaDatagrid-rowOdd': {
							backgroundColor: theme => theme.palette.grey[50],
						},
					}}
				>
					<TextField source='name' label='Name' />
					{isSuper(permissions) &&
						<ReferenceField source='group_id' label='Group' reference='grps' link={false} >
							<TextField source='displayName' />
						</ReferenceField>
					}
					<DeviceTypeField source='deviceType' label='Type' />
					<StatusField label='Status' />
					<DateField source='timeStarted' label='Started' showTime options={AppDateTimeFormat} />
					<DateField source='lastPing' label='Last ping' showTime options={AppDateTimeFormat} />
					<ShowButton />
				</Datagrid>
			)}
		</List>
	);
};

export default CubeList;
