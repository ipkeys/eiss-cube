import { Box } from '@mui/material';
import {
	TextInput,
	ReferenceInput,
	AutocompleteInput,
	Edit,
	TabbedForm,
	FormTab,
	maxLength,
	usePermissions,
	useRecordContext
} from 'react-admin';
import SettingsInput from '../common/SettingsInput';
import { isSuper } from '../common/Roles';

const CubeEditTitle = () => {
	const record = useRecordContext();
	if (!record) return null;

	return <span>Edit EISS™Cube - {record.name}</span>;
};

const validateName = maxLength(50);

const CubeEdit = () => {
	const { permissions } = usePermissions();

	return (
		<Edit title={<CubeEditTitle />} >
			<TabbedForm>
				<FormTab label='identity'>
					<TextInput disabled label='ID' source='deviceID' sx={{minWidth: '20em'}} />
					<Box display={'inline-flex'}>
						<Box flex={1} mr={2} minWidth={'20em'} >
							<TextInput label='Name' source='name' validate={validateName} fullWidth />
						</Box>
						<Box flex={1} minWidth={'20em'} >
							{isSuper(permissions) &&
								<ReferenceInput
									sort={{field: 'displayName', order: 'ASC'}}
									source="group_id"
									reference="grps"
								>
									<AutocompleteInput optionText='displayName' fullWidth />
								</ReferenceInput>
							}
						</Box>
					</Box>
				</FormTab>
				<FormTab label='customer'>
					<TextInput label='Customer ID' source='customerID' sx={{minWidth: '41em'}} />

					<Box display={'inline-flex'}>
						<Box flex={1} mr={2} minWidth={'20em'} >
							<TextInput label='Zone' source='zone' fullWidth />
						</Box>
						<Box flex={1} minWidth={'20em'} >
							<TextInput label='Subzone' source='subZone' fullWidth />
						</Box>
					</Box>
				</FormTab>
				<FormTab label='address'>
					<TextInput label='Address' source='address' sx={{minWidth: '41em'}} />

					<Box display={'inline-flex'}>
						<Box flex={1} mr={2} minWidth={'20em'} >
							<TextInput label='City, State' source='city' fullWidth />
						</Box>
						<Box flex={1} minWidth={'20em'} >
							<TextInput label='Zip Code' source='zipCode' fullWidth />
						</Box>
					</Box>
				</FormTab>
				<FormTab label='settings'>
					<SettingsInput />
				</FormTab>
			</TabbedForm>
		</Edit>
	);
};

export default CubeEdit;
