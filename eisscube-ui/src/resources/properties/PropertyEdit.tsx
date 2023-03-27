import { Box } from '@mui/material';
import {
	Edit,
	SimpleForm,
	SelectInput,
	TextInput,
	useRecordContext,
	required,
	maxLength
} from 'react-admin';
import { PropertyTypes } from '../common';

const PropertyEditTitle = () => {
	const record = useRecordContext();
	if (!record) return null;

	return <span>Edit Property - {record.name}</span>;
};

const validateValue = [required(), maxLength(20)];

const PropertyEdit = () => (
	<Edit title={<PropertyEditTitle />}
		redirect='list'
	>
		<SimpleForm>
			<SelectInput source='type' choices={PropertyTypes} sx={{minWidth: '20em'}} />

			<Box display={'inline-flex'}>
				<Box flex={1} mr={2} minWidth={'20em'} >
					<TextInput label='Name' source='name' validate={validateValue} fullWidth />
				</Box>
				<Box flex={1} minWidth={'20em'} >
					<TextInput label='Label' source='label' validate={validateValue} fullWidth />
				</Box>
			</Box>

			<TextInput label='Description' source='description' validate={[maxLength(50)]} sx={{minWidth: '41em'}} />
		</SimpleForm>
	</Edit>
);

export default PropertyEdit;
