import { Box, Divider } from '@mui/material';
import ChevronLeft from '@mui/icons-material/ChevronLeft';
import {
	Show,
	SimpleShowLayout,
	SelectField,
	TextField,
	TopToolbar,
	EditButton,
	ListButton,
	Labeled,
	usePermissions,
	useRecordContext
} from 'react-admin';
import { PropertyTypes } from '../common';
import { canEdit } from '../common/permissions';

const PropertyShowActions = (data: any) => {
	const { permissions } = usePermissions();
	const record = useRecordContext();

	return (
		<TopToolbar>
			<ListButton label="Back" icon={<ChevronLeft />} />
			{ canEdit(data, permissions) && <EditButton record={record} /> }
		</TopToolbar>
	);
};

const PropertyShowTitle = () => {
	const record = useRecordContext();
	if (!record) return null;

	return <span>View Property - {record.name}</span>;
};

const PropertyShow = (props: any) => (
	<Show
		title={<PropertyShowTitle />}
		actions={<PropertyShowActions {...props} />}
		{...props}
	>
		<SimpleShowLayout>
			<SelectField source='type' choices={PropertyTypes} />

			<Box display={'inline-flex'}>
				<Box flex={1} mr={1} minWidth={'20em'} >
					<Labeled label='Name' >
						<TextField source='name' />
					</Labeled>
				</Box>
				<Box flex={1} minWidth={'20em'} >
					<Labeled label='Label'>
						<TextField source='label' />
					</Labeled>
				</Box>
			</Box>

			<Divider />

			<TextField label='Description' source='description' />
		</SimpleShowLayout>
	</Show>
);

export default PropertyShow;
